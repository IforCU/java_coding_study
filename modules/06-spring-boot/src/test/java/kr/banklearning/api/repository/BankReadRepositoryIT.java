package kr.banklearning.api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.LimitView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.dto.BankViews.TransactionView;
import kr.banklearning.api.service.BankReadService;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 실제 PostgreSQL용 opt-in 검증. owner는 fixture 준비만, repository는 reader 역할만 사용한다. */
class BankReadRepositoryIT {
    @Test
    void readerMapsOwnedDataHistoryLimitsAndStatisticsWithoutChangingLegacyRows()
            throws SQLException {
        DataSource owner = ownerDataSource();
        JdbcBankReadRepository repository = new JdbcBankReadRepository(readerDataSource());
        Fixture fixture = createFixture(owner, repository);

        CustomerView customer = repository.customer(fixture.customer()).orElseThrow();
        assertEquals(fixture.customer(), customer.id());
        assertEquals("API가상고객 한글 ' 조회", customer.name());
        assertEquals("ACTIVE", customer.status());
        assertTrue(repository.customer(UUID.randomUUID()).isEmpty());

        AccountView account = repository.account(fixture.source()).orElseThrow();
        assertEquals(fixture.customer(), account.customerId());
        assertEquals(money("850"), account.balance());
        assertEquals(money("1000"), account.dailyLimit());
        assertEquals(2, account.version());
        assertTrue(repository.account(UUID.randomUUID()).isEmpty());
        assertEquals(money("150"), repository.dailyUsed(fixture.source(), fixture.date()));
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        repository.dailyUsed(fixture.source(), fixture.date().minusDays(1))));

        List<TransactionView> history = repository.transactions(fixture.source(), null, null, 10);
        assertEquals(
                List.of(fixture.secondLedger(), fixture.firstLedger()),
                history.stream().map(TransactionView::id).toList());
        assertEquals(fixture.cancelledRequest(), history.get(0).requestId());
        assertEquals(fixture.source(), history.get(0).accountId());
        assertEquals("WITHDRAWAL", history.get(0).type());
        assertEquals(money("50"), history.get(0).amount());
        assertEquals(money("850"), history.get(0).balanceAfter());
        assertEquals(fixture.secondTime().toInstant(), history.get(0).occurredAt().toInstant());
        assertEquals(fixture.firstTime().toInstant(), history.get(1).occurredAt().toInstant());
        assertEquals(
                List.of(fixture.secondLedger()),
                repository.transactions(fixture.source(), null, null, 1).stream()
                        .map(TransactionView::id)
                        .toList());
        assertEquals(
                List.of(fixture.firstLedger()),
                repository
                        .transactions(
                                fixture.source(), fixture.firstTime(), fixture.secondTime(), 10)
                        .stream()
                        .map(TransactionView::id)
                        .toList());
        assertEquals(2, repository.transactions(fixture.destination(), null, null, 10).size());

        StatisticsView statistics = repository.statistics(fixture.date(), fixture.date());
        assertEquals(fixture.date(), statistics.from());
        assertEquals(fixture.date(), statistics.to());
        assertEquals(fixture.before().transferCount() + 2, statistics.transferCount());
        assertEquals(
                0,
                fixture.before()
                        .totalAmount()
                        .add(money("150"))
                        .compareTo(statistics.totalAmount()));
        // CANCEL_REQUESTED도 완료된 자금 이동이다. PENDING 700원은 집계에 포함하지 않는다.
        assertTrue(repository.ready());
        assertEquals(money("850"), readBalance(owner, fixture.source()));
        assertEquals(money("250"), readBalance(owner, fixture.destination()));
        assertEquals(
                3L,
                countRows(
                        owner,
                        "SELECT count(*) FROM phase4.transfer_request WHERE source_id = ?",
                        fixture.source()));
        assertEquals(
                4L,
                countRows(
                        owner,
                        "SELECT count(*) FROM phase4.ledger WHERE account_id IN (?, ?)",
                        fixture.source(),
                        fixture.destination()));
    }

    @Test
    void databaseRoleHasSelectOnlyAndRejectsUpdateEvenInsideAnExplicitReadWriteTransaction()
            throws SQLException {
        try (Connection connection = readerDataSource().getConnection()) {
            for (String table :
                    List.of("customer", "account", "transfer_request", "ledger", "daily_usage")) {
                assertTrue(hasPrivilege(connection, "phase4." + table, "SELECT"));
                for (String forbidden :
                        List.of(
                                "INSERT",
                                "UPDATE",
                                "DELETE",
                                "TRUNCATE",
                                "REFERENCES",
                                "TRIGGER")) {
                    assertFalse(
                            hasPrivilege(connection, "phase4." + table, forbidden),
                            "Reader role unexpectedly has " + forbidden + " on phase4." + table);
                }
            }
            try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "SELECT rolsuper, rolcreatedb, rolcreaterole, rolinherit, rolreplication, rolbypassrls "
                                            + "FROM pg_roles WHERE rolname = current_user");
                    ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                for (String flag :
                        List.of(
                                "rolsuper",
                                "rolcreatedb",
                                "rolcreaterole",
                                "rolinherit",
                                "rolreplication",
                                "rolbypassrls")) {
                    assertFalse(
                            result.getBoolean(flag),
                            "Reader role has an unexpected role attribute: " + flag);
                }
            }

            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement =
                        connection.prepareStatement("SET TRANSACTION READ WRITE")) {
                    statement.execute();
                }
                try (PreparedStatement statement =
                                connection.prepareStatement("SHOW transaction_read_only");
                        ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("off", result.getString(1));
                }
                try (PreparedStatement denied =
                        connection.prepareStatement(
                                "UPDATE phase4.account SET balance = balance WHERE false")) {
                    SQLException failure = assertThrows(SQLException.class, denied::executeUpdate);
                    assertEquals("42501", failure.getSQLState());
                }
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void repeatableReadLimitUsesOneSnapshotWhileAnOwnerCommitsBetweenItsTwoSelects()
            throws SQLException {
        DataSource owner = ownerDataSource();
        DataSource reader = readerDataSource();
        JdbcBankReadRepository repository = new JdbcBankReadRepository(reader);
        Fixture fixture = createFixture(owner, repository);
        AtomicBoolean committedBetweenReads = new AtomicBoolean();
        BankReadRepository coordinated =
                (BankReadRepository)
                        Proxy.newProxyInstance(
                                BankReadRepositoryIT.class.getClassLoader(),
                                new Class<?>[] {BankReadRepository.class},
                                (proxy, method, arguments) -> {
                                    Object result;
                                    try {
                                        result = method.invoke(repository, arguments);
                                    } catch (InvocationTargetException exception) {
                                        throw exception.getCause();
                                    }
                                    if (method.getName().equals("account")
                                            && committedBetweenReads.compareAndSet(false, true)) {
                                        assertTrue(
                                                TransactionSynchronizationManager
                                                        .isActualTransactionActive());
                                        assertTrue(
                                                TransactionSynchronizationManager
                                                        .isCurrentTransactionReadOnly());
                                        assertEquals(
                                                Integer.valueOf(
                                                        Connection.TRANSACTION_REPEATABLE_READ),
                                                TransactionSynchronizationManager
                                                        .getCurrentTransactionIsolationLevel());
                                        // reader의 첫 SELECT 뒤 별도 owner 연결에서 이 fixture만 변경한다.
                                        commitNewLimitAndUsage(owner, fixture);
                                    }
                                    return result;
                                });
        Clock clock =
                Clock.fixed(
                        fixture.date().atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                        ZoneId.of("Asia/Seoul"));
        BankReadService target = new BankReadService(coordinated, clock);
        DataSourceTransactionManager manager = new DataSourceTransactionManager(reader);
        manager.setDefaultTimeout(10);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        interceptor.afterPropertiesSet();
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(interceptor);
        BankReadService service = (BankReadService) proxy.getProxy();

        LimitView consistent = service.limit(fixture.source(), "admin", true);

        assertTrue(committedBetweenReads.get());
        assertEquals(money("1000"), consistent.dailyLimit());
        assertEquals(money("150"), consistent.used());
        assertEquals(money("850"), consistent.remaining());
        LimitView nextTransaction = service.limit(fixture.source(), "admin", true);
        assertEquals(money("2000"), nextTransaction.dailyLimit());
        assertEquals(money("300"), nextTransaction.used());
        assertEquals(money("1700"), nextTransaction.remaining());
    }

    private static void commitNewLimitAndUsage(DataSource owner, Fixture fixture)
            throws SQLException {
        try (Connection connection = owner.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement limit =
                        connection.prepareStatement(
                                "UPDATE phase4.account SET balance = 700, daily_limit = 2000, version = version + 1 WHERE id = ?")) {
                    limit.setObject(1, fixture.source());
                    assertEquals(1, limit.executeUpdate());
                }
                try (PreparedStatement credit =
                        connection.prepareStatement(
                                "UPDATE phase4.account SET balance = 400, version = version + 1 WHERE id = ?")) {
                    credit.setObject(1, fixture.destination());
                    assertEquals(1, credit.executeUpdate());
                }
                UUID request = UUID.randomUUID();
                OffsetDateTime time = fixture.secondTime().plusMinutes(30);
                insertRequest(
                        connection,
                        request,
                        fixture.source(),
                        fixture.destination(),
                        "150",
                        "COMPLETED",
                        fixture.date(),
                        time);
                insertLedger(
                        connection,
                        UUID.randomUUID(),
                        request,
                        fixture.source(),
                        "WITHDRAWAL",
                        "150",
                        "700",
                        time);
                insertLedger(
                        connection,
                        UUID.randomUUID(),
                        request,
                        fixture.destination(),
                        "DEPOSIT",
                        "150",
                        "400",
                        time);
                try (PreparedStatement used =
                        connection.prepareStatement(
                                "UPDATE phase4.daily_usage SET used = 300 WHERE account_id = ? AND business_date = ?")) {
                    used.setObject(1, fixture.source());
                    used.setObject(2, fixture.date());
                    assertEquals(1, used.executeUpdate());
                }
                connection.commit();
            } catch (SQLException | RuntimeException | Error failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    private static Fixture createFixture(DataSource owner, JdbcBankReadRepository reader)
            throws SQLException {
        UUID customer = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        UUID completed = UUID.randomUUID();
        UUID cancelled = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        UUID firstLedger = UUID.randomUUID();
        UUID secondLedger = UUID.randomUUID();
        // 일반 실행의 오늘 업무일과 떨어진 가상 날짜로 반복/다른 IT 데이터의 간섭을 줄인다.
        LocalDate date =
                LocalDate.of(2200, 1, 1)
                        .plusDays(Integer.toUnsignedLong(customer.hashCode()) % 300000L);
        OffsetDateTime firstTime =
                date.atTime(9, 0).atOffset(ZoneOffset.ofHours(9)).plusNanos(123456000);
        OffsetDateTime secondTime = firstTime.plusHours(1);
        StatisticsView before = reader.statistics(date, date);
        try (Connection connection = owner.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "INSERT INTO phase4.customer (id, name, status) VALUES (?, ?, 'ACTIVE')")) {
                    statement.setObject(1, customer);
                    statement.setString(2, "API가상고객 한글 ' 조회");
                    assertEquals(1, statement.executeUpdate());
                }
                insertAccount(connection, source, customer, "850");
                insertAccount(connection, destination, customer, "250");
                insertRequest(
                        connection,
                        completed,
                        source,
                        destination,
                        "100",
                        "COMPLETED",
                        date,
                        firstTime);
                insertRequest(
                        connection,
                        cancelled,
                        source,
                        destination,
                        "50",
                        "CANCEL_REQUESTED",
                        date,
                        secondTime);
                insertRequest(
                        connection,
                        pending,
                        source,
                        destination,
                        "700",
                        "PENDING",
                        date,
                        secondTime.plusHours(1));
                insertLedger(
                        connection,
                        firstLedger,
                        completed,
                        source,
                        "WITHDRAWAL",
                        "100",
                        "900",
                        firstTime);
                insertLedger(
                        connection,
                        UUID.randomUUID(),
                        completed,
                        destination,
                        "DEPOSIT",
                        "100",
                        "200",
                        firstTime);
                insertLedger(
                        connection,
                        secondLedger,
                        cancelled,
                        source,
                        "WITHDRAWAL",
                        "50",
                        "850",
                        secondTime);
                insertLedger(
                        connection,
                        UUID.randomUUID(),
                        cancelled,
                        destination,
                        "DEPOSIT",
                        "50",
                        "250",
                        secondTime);
                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "INSERT INTO phase4.daily_usage (account_id, business_date, used) VALUES (?, ?, ?)")) {
                    statement.setObject(1, source);
                    statement.setObject(2, date);
                    statement.setBigDecimal(3, money("150"));
                    assertEquals(1, statement.executeUpdate());
                }
                connection.commit();
            } catch (SQLException | RuntimeException | Error failure) {
                connection.rollback();
                throw failure;
            }
        }
        // reader는 fixture를 생성하지 않는다. 성공 fixture는 삭제하지 않고 보존한다.
        return new Fixture(
                customer,
                source,
                destination,
                cancelled,
                firstLedger,
                secondLedger,
                date,
                firstTime,
                secondTime,
                before);
    }

    private static void insertAccount(Connection connection, UUID id, UUID customer, String balance)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "INSERT INTO phase4.account (id, customer_id, balance, daily_limit, version) VALUES (?, ?, ?, 1000, 2)")) {
            statement.setObject(1, id);
            statement.setObject(2, customer);
            statement.setBigDecimal(3, money(balance));
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void insertRequest(
            Connection connection,
            UUID id,
            UUID source,
            UUID destination,
            String amount,
            String status,
            LocalDate date,
            OffsetDateTime occurredAt)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "INSERT INTO phase4.transfer_request "
                                + "(request_id, source_id, destination_id, amount, status, business_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setObject(2, source);
            statement.setObject(3, destination);
            statement.setBigDecimal(4, money(amount));
            statement.setString(5, status);
            statement.setObject(6, date);
            statement.setObject(7, occurredAt);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void insertLedger(
            Connection connection,
            UUID id,
            UUID request,
            UUID account,
            String type,
            String amount,
            String balanceAfter,
            OffsetDateTime occurredAt)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "INSERT INTO phase4.ledger (id, request_id, account_id, type, amount, balance_after, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setObject(2, request);
            statement.setObject(3, account);
            statement.setString(4, type);
            statement.setBigDecimal(5, money(amount));
            statement.setBigDecimal(6, money(balanceAfter));
            statement.setObject(7, occurredAt);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static boolean hasPrivilege(Connection connection, String table, String privilege)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT has_table_privilege(current_user, ?, ?)")) {
            statement.setString(1, table);
            statement.setString(2, privilege);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getBoolean(1);
            }
        }
    }

    private static BigDecimal readBalance(DataSource owner, UUID account) throws SQLException {
        try (Connection connection = owner.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT balance FROM phase4.account WHERE id = ?")) {
            statement.setObject(1, account);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getBigDecimal(1);
            }
        }
    }

    private static long countRows(DataSource owner, String sql, UUID... ids) throws SQLException {
        try (Connection connection = owner.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.length; index++)
                statement.setObject(index + 1, ids[index]);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }

    private static PGSimpleDataSource ownerDataSource() {
        return source(
                requiredEnvironment("DB_URL"),
                requiredEnvironment("DB_USER"),
                requiredEnvironment("DB_PASSWORD"));
    }

    private static PGSimpleDataSource readerDataSource() {
        return source(
                requiredEnvironment("DB_URL"),
                System.getenv().getOrDefault("API_DB_USER", "bank_learning_api"),
                System.getenv().getOrDefault("API_DB_PASSWORD", "learning-api-only-password"));
    }

    private static PGSimpleDataSource source(String url, String user, String password) {
        if (!url.startsWith("jdbc:postgresql://") || user.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "BankReadRepositoryIT requires local PostgreSQL connection settings.");
        }
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(url);
        source.setUser(user);
        source.setPassword(password);
        source.setConnectTimeout(3);
        source.setSocketTimeout(5);
        source.setApplicationName("phase6-reader-integration-tests");
        return source;
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "BankReadRepositoryIT requires "
                            + key
                            + "; use the integration profile with V006 applied and separate owner/reader credentials.");
        }
        return value;
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    private record Fixture(
            UUID customer,
            UUID source,
            UUID destination,
            UUID cancelledRequest,
            UUID firstLedger,
            UUID secondLedger,
            LocalDate date,
            OffsetDateTime firstTime,
            OffsetDateTime secondTime,
            StatisticsView before) {}
}
