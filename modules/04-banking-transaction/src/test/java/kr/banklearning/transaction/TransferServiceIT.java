package kr.banklearning.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import kr.banklearning.transaction.domain.LedgerEntry;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.domain.TransferReceipt;
import kr.banklearning.transaction.repository.JdbcTransferRepository;
import kr.banklearning.transaction.service.BankProblem;
import kr.banklearning.transaction.service.FailureHook;
import kr.banklearning.transaction.service.TransferService;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * 실제 PostgreSQL + Spring 트랜잭션 프록시용 opt-in 검증.
 * 각 테스트는 새로운 UUID의 가상 행만 추가하고 성공한 fixture/거래를 남긴다.
 * 기존 행 삭제, 데이터 초기화, DDL을 수행하지 않는다.
 */
class TransferServiceIT {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 25);

    @Test
    void failureAfterDebitRollsBackBothBalancesRequestLedgerDailyUsageAndVersions()
            throws SQLException {
        Fixture fixture = createFixture("1000", "100", "1000");
        Snapshot before = snapshot(fixture);
        AtomicBoolean reachedDebit = new AtomicBoolean();
        TransferService service =
                transactionalService(
                        fixture,
                        () -> {
                            reachedDebit.set(true);
                            assertEquals(
                                    money("900"),
                                    fixture.jdbc()
                                            .queryForObject(
                                                    "SELECT balance FROM phase4.account WHERE id = ?",
                                                    BigDecimal.class,
                                                    fixture.source()));
                            throw new IllegalStateException("Injected failure after debit.");
                        });
        TransferCommand command = command(fixture, "100");

        assertThrows(IllegalStateException.class, () -> service.transfer(command));

        assertTrue(reachedDebit.get());
        assertEquals(before, snapshot(fixture));
        assertEquals(0L, requestCount(fixture, command.requestId()));
    }

    @Test
    void concurrentDuplicateRequestsAndLaterReplayMoveMoneyOnlyOnce() throws Exception {
        Fixture fixture = createFixture("1000", "100", "1000");
        TransferService service = transactionalService(fixture, () -> {});
        TransferCommand command = command(fixture, "125");

        List<Outcome> outcomes = concurrently(service, command, command);

        assertTrue(outcomes.stream().allMatch(outcome -> outcome.failure() == null));
        assertEquals(outcomes.get(0).receipt(), outcomes.get(1).receipt());
        assertEquals("COMPLETED", outcomes.get(0).receipt().status());
        assertEquals(BUSINESS_DATE, outcomes.get(0).receipt().businessDate());
        assertEquals(
                new Snapshot(money("875"), money("225"), 1, 1, money("125"), 1, 1, 2),
                snapshot(fixture));
        assertLedgerPair(service, command);
        Snapshot completed = snapshot(fixture);
        List<LedgerEntry> ledger = service.ledger(command.requestId());

        assertEquals(outcomes.get(0).receipt(), service.transfer(command));
        assertEquals(completed, snapshot(fixture));
        assertEquals(ledger, service.ledger(command.requestId()));
    }

    @Test
    void concurrentWithdrawalsCannotOverdrawTheSameAccountOrDoubleItsDailyUsage() throws Exception {
        Fixture fixture = createFixture("1000", "100", "800");
        TransferService service = transactionalService(fixture, () -> {});
        TransferCommand first = command(fixture, "700");
        TransferCommand second = command(fixture, "700");

        List<Outcome> outcomes = concurrently(service, first, second);

        assertEquals(1L, outcomes.stream().filter(outcome -> outcome.failure() == null).count());
        assertEquals(1L, outcomes.stream().filter(outcome -> outcome.failure() != null).count());
        assertEquals(
                409,
                outcomes.stream()
                        .filter(outcome -> outcome.failure() != null)
                        .findFirst()
                        .orElseThrow()
                        .failure()
                        .status());
        assertEquals(
                new Snapshot(money("300"), money("800"), 1, 1, money("700"), 1, 1, 2),
                snapshot(fixture));
        assertTrue(snapshot(fixture).used().compareTo(money("800")) <= 0);
    }

    @Test
    void realDatabaseConstraintFailureAfterDebitRollsBackTheWholeTransfer() throws SQLException {
        Fixture fixture = createFixture("1000", "100", "1000");
        Snapshot before = snapshot(fixture);
        AtomicBoolean reachedDatabaseFailure = new AtomicBoolean();
        TransferService service =
                transactionalService(
                        fixture,
                        () -> {
                            reachedDatabaseFailure.set(true);
                            // 같은 DataSource의 JdbcTemplate이 서비스 트랜잭션에 참여한다.
                            // 자기 fixture 계좌에서 CHECK 위반을 발생시켜 실제 DB 오류 경로를 검증한다.
                            fixture.jdbc()
                                    .update(
                                            "UPDATE phase4.account SET balance = ? WHERE id = ?",
                                            money("-1"),
                                            fixture.source());
                        });
        TransferCommand command = command(fixture, "100");

        assertThrows(DataAccessException.class, () -> service.transfer(command));

        assertTrue(reachedDatabaseFailure.get());
        assertEquals(before, snapshot(fixture));
        assertEquals(0L, requestCount(fixture, command.requestId()));
    }

    @Test
    void retryAfterCommittedResponseIsLostReturnsTheStoredReceiptWithoutNewMutations()
            throws SQLException {
        Fixture fixture = createFixture("1000", "100", "1000");
        TransferService service = transactionalService(fixture, () -> {});
        TransferCommand command = command(fixture, "100");
        // 서버 commit 후 호출자가 응답을 받지 못한 상황을 모델링한다. HTTP 단절 시험은 아니다.
        service.transfer(command);
        TransferReceipt stored = service.get(command.requestId());
        Snapshot committed = snapshot(fixture);
        List<LedgerEntry> ledger = service.ledger(command.requestId());

        TransferReceipt retry = service.transfer(command);

        assertEquals(stored, retry);
        assertEquals(committed, snapshot(fixture));
        assertEquals(ledger, service.ledger(command.requestId()));
        assertEquals(
                new Snapshot(money("900"), money("200"), 1, 1, money("100"), 1, 1, 2), committed);
    }

    @Test
    void dailyLimitPayloadConflictAndCancellationPreserveCommittedMoney() throws SQLException {
        Fixture fixture = createFixture("1000", "100", "150");
        TransferService service = transactionalService(fixture, () -> {});
        TransferCommand first = command(fixture, "100");
        service.transfer(first);
        Snapshot committed = snapshot(fixture);
        List<LedgerEntry> ledger = service.ledger(first.requestId());
        TransferCommand exceeding = command(fixture, "100");

        BankProblem daily = assertThrows(BankProblem.class, () -> service.transfer(exceeding));
        assertEquals("DAILY_LIMIT", daily.code());
        assertEquals(0L, requestCount(fixture, exceeding.requestId()));
        BankProblem conflict =
                assertThrows(
                        BankProblem.class,
                        () ->
                                service.transfer(
                                        new TransferCommand(
                                                first.requestId(),
                                                first.sourceId(),
                                                first.destinationId(),
                                                money("101"))));
        assertEquals("REQUEST_CONFLICT", conflict.code());
        assertEquals(committed, snapshot(fixture));

        TransferReceipt cancellation = service.requestCancellation(first.requestId());
        assertEquals("CANCEL_REQUESTED", cancellation.status());
        assertEquals(cancellation, service.requestCancellation(first.requestId()));
        assertEquals(cancellation, service.transfer(first));
        assertEquals(committed, snapshot(fixture));
        assertEquals(ledger, service.ledger(first.requestId()));
    }

    @Test
    void oppositeDirectionConcurrentTransfersUseTheSameLockOrderWithoutLosingMoney()
            throws Exception {
        Fixture fixture = createFixture("1000", "1000", "1000");
        TransferService service = transactionalService(fixture, () -> {});
        TransferCommand forward = command(fixture, "100");
        TransferCommand reverse =
                new TransferCommand(
                        UUID.randomUUID(), fixture.destination(), fixture.source(), money("100"));

        List<Outcome> outcomes = concurrently(service, forward, reverse);

        assertTrue(outcomes.stream().allMatch(outcome -> outcome.failure() == null));
        Snapshot state = snapshot(fixture);
        assertEquals(money("1000"), state.sourceBalance());
        assertEquals(money("1000"), state.destinationBalance());
        assertEquals(2, state.sourceVersion());
        assertEquals(2, state.destinationVersion());
        assertEquals(money("100"), state.used());
        assertEquals(
                money("100"),
                fixture.jdbc()
                        .queryForObject(
                                "SELECT used FROM phase4.daily_usage WHERE account_id = ? AND business_date = ?",
                                BigDecimal.class,
                                fixture.destination(),
                                BUSINESS_DATE));
        assertEquals(4L, state.ledgerRows());
        assertLedgerPair(service, forward);
        assertLedgerPair(service, reverse);
    }

    private static TransferService transactionalService(Fixture fixture, FailureHook hook) {
        TransferService target =
                new TransferService(new JdbcTransferRepository(fixture.dataSource()), CLOCK, hook);
        DataSourceTransactionManager manager =
                new DataSourceTransactionManager(fixture.dataSource());
        manager.setDefaultTimeout(12);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        interceptor.afterPropertiesSet();
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(interceptor);
        TransferService result = (TransferService) factory.getProxy();
        assertTrue(AopUtils.isAopProxy(result));
        return result;
    }

    private static List<Outcome> concurrently(
            TransferService service, TransferCommand first, TransferCommand second)
            throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Outcome> left = workers.submit(() -> invokeAtBarrier(service, first, start));
            Future<Outcome> right = workers.submit(() -> invokeAtBarrier(service, second, start));
            return List.of(left.get(20, TimeUnit.SECONDS), right.get(20, TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
            assertTrue(
                    workers.awaitTermination(15, TimeUnit.SECONDS),
                    "Concurrent transfer workers did not stop.");
        }
    }

    private static Outcome invokeAtBarrier(
            TransferService service, TransferCommand command, CyclicBarrier start)
            throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            return new Outcome(service.transfer(command), null);
        } catch (BankProblem failure) {
            return new Outcome(null, failure);
        }
    }

    private static Fixture createFixture(
            String sourceBalance, String destinationBalance, String dailyLimit)
            throws SQLException {
        PGSimpleDataSource source = dataSource();
        UUID customer = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "INSERT INTO phase4.customer (id, name, status) VALUES (?, ?, 'ACTIVE')")) {
                    statement.setObject(1, customer);
                    statement.setString(2, "IT가상고객-" + customer);
                    assertEquals(1, statement.executeUpdate());
                }
                insertAccount(connection, sourceId, customer, sourceBalance, dailyLimit);
                insertAccount(connection, destinationId, customer, destinationBalance, dailyLimit);
                connection.commit();
            } catch (SQLException | RuntimeException | Error failure) {
                connection.rollback();
                throw failure;
            }
        }
        // 성공 fixture는 의도적으로 보존한다. 모든 관찰 쿼리는 이 UUID 범위로 제한한다.
        return new Fixture(source, new JdbcTemplate(source), sourceId, destinationId);
    }

    private static void insertAccount(
            Connection connection, UUID id, UUID customerId, String balance, String limit)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "INSERT INTO phase4.account (id, customer_id, balance, daily_limit, version) VALUES (?, ?, ?, ?, 0)")) {
            statement.setObject(1, id);
            statement.setObject(2, customerId);
            statement.setBigDecimal(3, money(balance));
            statement.setBigDecimal(4, money(limit));
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static PGSimpleDataSource dataSource() {
        String url = requiredEnvironment("DB_URL");
        String user = requiredEnvironment("DB_USER");
        String password = requiredEnvironment("DB_PASSWORD");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("TransferServiceIT requires a PostgreSQL DB_URL.");
        }
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(url);
        source.setUser(user);
        source.setPassword(password);
        source.setConnectTimeout(3);
        source.setSocketTimeout(10);
        source.setApplicationName("phase4-integration-tests");
        return source;
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "TransferServiceIT requires "
                            + key
                            + "; run the integration profile against the migrated local PostgreSQL database.");
        }
        return value;
    }

    private static Snapshot snapshot(Fixture fixture) {
        JdbcTemplate jdbc = fixture.jdbc();
        BigDecimal sourceBalance =
                jdbc.queryForObject(
                        "SELECT balance FROM phase4.account WHERE id = ?",
                        BigDecimal.class,
                        fixture.source());
        BigDecimal destinationBalance =
                jdbc.queryForObject(
                        "SELECT balance FROM phase4.account WHERE id = ?",
                        BigDecimal.class,
                        fixture.destination());
        long sourceVersion =
                jdbc.queryForObject(
                        "SELECT version FROM phase4.account WHERE id = ?",
                        Long.class,
                        fixture.source());
        long destinationVersion =
                jdbc.queryForObject(
                        "SELECT version FROM phase4.account WHERE id = ?",
                        Long.class,
                        fixture.destination());
        BigDecimal used =
                jdbc.queryForObject(
                        "SELECT COALESCE(sum(used), 0) FROM phase4.daily_usage WHERE account_id = ? AND business_date = ?",
                        BigDecimal.class,
                        fixture.source(),
                        BUSINESS_DATE);
        long usageRows =
                jdbc.queryForObject(
                        "SELECT count(*) FROM phase4.daily_usage WHERE account_id = ? AND business_date = ?",
                        Long.class,
                        fixture.source(),
                        BUSINESS_DATE);
        long requestRows =
                jdbc.queryForObject(
                        "SELECT count(*) FROM phase4.transfer_request WHERE source_id = ?",
                        Long.class,
                        fixture.source());
        long ledgerRows =
                jdbc.queryForObject(
                        "SELECT count(*) FROM phase4.ledger WHERE account_id IN (?, ?)",
                        Long.class,
                        fixture.source(),
                        fixture.destination());
        return new Snapshot(
                sourceBalance,
                destinationBalance,
                sourceVersion,
                destinationVersion,
                used,
                usageRows,
                requestRows,
                ledgerRows);
    }

    private static long requestCount(Fixture fixture, UUID requestId) {
        return fixture.jdbc()
                .queryForObject(
                        "SELECT count(*) FROM phase4.transfer_request WHERE request_id = ?",
                        Long.class,
                        requestId);
    }

    private static void assertLedgerPair(TransferService service, TransferCommand command) {
        List<LedgerEntry> entries = service.ledger(command.requestId());
        assertEquals(2, entries.size());
        assertEquals(
                1L,
                entries.stream()
                        .filter(
                                entry ->
                                        entry.accountId().equals(command.sourceId())
                                                && entry.type().equals("WITHDRAWAL")
                                                && entry.amount().compareTo(command.amount()) == 0)
                        .count());
        assertEquals(
                1L,
                entries.stream()
                        .filter(
                                entry ->
                                        entry.accountId().equals(command.destinationId())
                                                && entry.type().equals("DEPOSIT")
                                                && entry.amount().compareTo(command.amount()) == 0)
                        .count());
    }

    private static TransferCommand command(Fixture fixture, String amount) {
        return new TransferCommand(
                UUID.randomUUID(), fixture.source(), fixture.destination(), money(amount));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private record Fixture(
            DataSource dataSource, JdbcTemplate jdbc, UUID source, UUID destination) {}

    private record Outcome(TransferReceipt receipt, BankProblem failure) {}

    private record Snapshot(
            BigDecimal sourceBalance,
            BigDecimal destinationBalance,
            long sourceVersion,
            long destinationVersion,
            BigDecimal used,
            long usageRows,
            long requestRows,
            long ledgerRows) {}
}
