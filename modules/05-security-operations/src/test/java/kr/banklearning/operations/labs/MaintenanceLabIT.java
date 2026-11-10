package kr.banklearning.operations.labs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * 실제 PG 장애 재현 전용 opt-in 테스트. PASS는 알려진 증상 관찰 성공이다.
 * 정상 앱 스키마를 사용하지 않으며 phase5_lab의 새 UUID fixture만 추가하고 보존한다.
 */
class MaintenanceLabIT {
    @Test
    void wrongSqlConditionLeaksAnotherFixtureOwnersAccount() throws SQLException {
        LabFixture fixture = createFixture("100", "200", "900");
        List<LabAccount> observed = new ArrayList<>();
        try (Connection connection = fixture.source().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, owner_id, balance FROM phase5_lab.account "
                             + "WHERE id IN (?, ?, ?) AND balance >= 0 ORDER BY id")) {
            // 자체 fixture의 ID 범위는 지키지만 현재 사용자 owner_id 조건이 빠진 장애다.
            statement.setObject(1, fixture.first());
            statement.setObject(2, fixture.second());
            statement.setObject(3, fixture.other());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) observed.add(new LabAccount(result.getObject("id", UUID.class),
                        result.getObject("owner_id", UUID.class), result.getBigDecimal("balance")));
            }
        }

        assertEquals(3, observed.size());
        assertEquals(2L, observed.stream().filter(account -> account.owner().equals(fixture.owner())).count());
        assertTrue(observed.stream().anyMatch(account -> account.id().equals(fixture.other())
                && !account.owner().equals(fixture.owner())));
    }

    @Test
    void realPoolStarvationTimesOutUntilTheBorrowedConnectionIsReturned() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setDataSource(dataSource());
        config.setPoolName("maintenance-lab-pool");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(3000);
        config.setValidationTimeout(250);
        config.setInitializationFailTimeout(-1);
        try (HikariDataSource pool = new HikariDataSource(config)) {
            try (Connection held = pool.getConnection()) {
                assertTrue(held.isValid(2));
                pool.getHikariConfigMXBean().setConnectionTimeout(250);

                assertThrows(SQLTransientConnectionException.class, pool::getConnection);

                assertEquals(1, pool.getHikariPoolMXBean().getActiveConnections());
                assertEquals(0, pool.getHikariPoolMXBean().getIdleConnections());
            }
            // 재현 fixture의 lease를 반환한 뒤 풀 자체는 다시 사용할 수 있어야 한다.
            try (Connection recovered = pool.getConnection()) {
                assertTrue(recovered.isValid(2));
            }
        }
    }

    @Test
    void statementTimeoutCancelsTheSlowQuery() throws SQLException {
        try (Connection connection = dataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement budget = connection.prepareStatement("SET LOCAL statement_timeout = '100ms'")) {
                    budget.execute();
                }
                try (PreparedStatement slow = connection.prepareStatement("SELECT pg_sleep(0.5)")) {
                    SQLException cancelled = assertThrows(SQLException.class, () -> {
                        try (ResultSet ignored = slow.executeQuery()) {
                            throw new AssertionError("The lab slow query was not cancelled.");
                        }
                    });
                    assertEquals("57014", cancelled.getSQLState());
                }
            } finally {
                // timeout 뒤 실패 상태와 SET LOCAL 설정을 같은 트랜잭션에서 정리한다.
                connection.rollback();
            }
        }
    }

    @Test
    void wrongAutoCommitScopePersistsOnlyTheDebit() throws SQLException {
        LabFixture fixture = createFixture("1000", "100", "900");

        SQLException injected = assertThrows(SQLException.class,
                () -> LabAutoCommitRunner.debitThenFail(fixture.source(), fixture.first(), new BigDecimal("100")));

        assertEquals("L0001", injected.getSQLState());
        BigDecimal source = readBalance(fixture.source(), fixture.first());
        BigDecimal destination = readBalance(fixture.source(), fixture.second());
        assertEquals(new BigDecimal("900"), source);
        assertEquals(new BigDecimal("100"), destination);
        assertEquals(new BigDecimal("1000"), source.add(destination));
        assertNotEquals(new BigDecimal("1100"), source.add(destination));
        // 부분 실패 결과는 가상 fixture에만 남긴다. 정상 앱 데이터나 seed는 변경하지 않는다.
    }

    private static LabFixture createFixture(String firstBalance, String secondBalance, String otherBalance)
            throws SQLException {
        DataSource source = dataSource();
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertAccount(connection, first, owner, firstBalance);
                insertAccount(connection, second, owner, secondBalance);
                insertAccount(connection, other, otherOwner, otherBalance);
                connection.commit();
            } catch (SQLException | RuntimeException | Error failure) {
                connection.rollback();
                throw failure;
            }
        }
        return new LabFixture(source, owner, first, second, other);
    }

    private static void insertAccount(Connection connection, UUID id, UUID owner, String balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO phase5_lab.account (id, owner_id, balance) VALUES (?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setObject(2, owner);
            statement.setBigDecimal(3, new BigDecimal(balance));
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static BigDecimal readBalance(DataSource source, UUID id) throws SQLException {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT balance FROM phase5_lab.account WHERE id = ?")) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                BigDecimal balance = result.getBigDecimal("balance");
                assertFalse(result.next());
                return balance;
            }
        }
    }

    private static PGSimpleDataSource dataSource() {
        String url = requiredEnvironment("DB_URL");
        String user = requiredEnvironment("DB_USER");
        String password = requiredEnvironment("DB_PASSWORD");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("MaintenanceLabIT requires a PostgreSQL DB_URL.");
        }
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(url);
        source.setUser(user);
        source.setPassword(password);
        source.setConnectTimeout(3);
        source.setSocketTimeout(5);
        source.setApplicationName("phase5-isolated-maintenance-labs");
        return source;
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("MaintenanceLabIT requires " + key
                    + "; use the integration profile with the migrated local PostgreSQL database.");
        }
        return value;
    }

    private record LabFixture(DataSource source, UUID owner, UUID first, UUID second, UUID other) {}
    private record LabAccount(UUID id, UUID owner, BigDecimal balance) {}

    private static final class LabAutoCommitRunner {
        private static void debitThenFail(DataSource source, UUID account, BigDecimal amount) throws SQLException {
            try (Connection connection = source.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE phase5_lab.account SET balance = balance - ? WHERE id = ?")) {
                assertTrue(connection.getAutoCommit());
                statement.setBigDecimal(1, amount);
                statement.setObject(2, account);
                assertEquals(1, statement.executeUpdate());
                throw new SQLException("Injected lab failure after an auto-committed debit.", "L0001");
            }
        }
    }
}
