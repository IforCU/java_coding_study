package kr.banklearning.servlet.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import kr.banklearning.servlet.dto.BankViews.TransactionView;
import org.junit.jupiter.api.Test;

/** 실제 PostgreSQL 전용 opt-in 검증. 기본 Surefire 실행에는 포함하지 않는다. */
class JdbcBankDaoIT {
    @Test
    void preservesUnicodeAndSqlLikeInputAsLiteralCustomerData() throws SQLException {
        try (TestTransaction transaction = openTransaction()) {
            JdbcBankDao dao = transaction.dao();
            UUID id = UUID.randomUUID();
            String name = "가상고객 한글 ' OR '1'='1 <script>";
            CustomerView expected = new CustomerView(id, name, "ACTIVE");

            dao.insertCustomer(expected);

            assertEquals(expected, dao.customer(id).orElseThrow());
            assertEquals(
                    List.of(expected),
                    dao.customers().stream().filter(customer -> customer.id().equals(id)).toList());
            assertTrue(dao.customer(UUID.randomUUID()).isEmpty());
            assertTrue(dao.ready());
        }
    }

    @Test
    void enforcesCustomerForeignKeyAndMapsOrderedHistoryWithExactAmountsAndInstants()
            throws SQLException {
        try (TestTransaction transaction = openTransaction()) {
            Connection connection = transaction.connection();
            JdbcBankDao dao = transaction.dao();
            UUID customerId = UUID.randomUUID();
            UUID otherCustomerId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID otherAccountId = UUID.randomUUID();
            dao.insertCustomer(new CustomerView(customerId, "가상 이력 고객", "ACTIVE"));
            dao.insertCustomer(new CustomerView(otherCustomerId, "가상 별도 고객", "ACTIVE"));
            dao.insertAccount(new AccountView(accountId, customerId, BigDecimal.ZERO));
            dao.insertAccount(new AccountView(otherAccountId, otherCustomerId, BigDecimal.ZERO));

            List<AccountView> accounts = dao.accounts(customerId);
            assertEquals(1, accounts.size());
            assertEquals(accountId, accounts.get(0).id());
            assertEquals(customerId, accounts.get(0).customerId());
            assertEquals(0, BigDecimal.ZERO.compareTo(accounts.get(0).balance()));
            assertEquals(
                    0, BigDecimal.ZERO.compareTo(dao.account(accountId).orElseThrow().balance()));
            assertTrue(dao.account(UUID.randomUUID()).isEmpty());
            assertTrue(dao.history(accountId).isEmpty());

            // PostgreSQL은 제약 위반 뒤 트랜잭션이 실패 상태가 되므로 savepoint로 복구한다.
            Savepoint beforeInvalidAccount = connection.setSavepoint();
            UUID orphanAccountId = UUID.randomUUID();
            try {
                assertThrows(
                        DataAccessException.class,
                        () ->
                                dao.insertAccount(
                                        new AccountView(
                                                orphanAccountId,
                                                UUID.randomUUID(),
                                                BigDecimal.ZERO)));
            } finally {
                connection.rollback(beforeInvalidAccount);
                connection.releaseSavepoint(beforeInvalidAccount);
            }
            assertTrue(dao.account(orphanAccountId).isEmpty());

            OffsetDateTime firstTime = OffsetDateTime.parse("2026-10-25T15:04:05.123456+09:00");
            OffsetDateTime secondTime = firstTime.plusSeconds(1);
            UUID firstHistoryId = UUID.randomUUID();
            UUID secondHistoryId = UUID.randomUUID();
            // 역순으로 저장해 DAO의 occurred_at 정렬을 실제 DB에서 확인한다.
            insertHistory(connection, secondHistoryId, accountId, "75", "200", secondTime);
            insertHistory(connection, firstHistoryId, accountId, "125", "125", firstTime);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "UPDATE phase2.account SET balance = ? WHERE id = ?")) {
                statement.setBigDecimal(1, new BigDecimal("200"));
                statement.setObject(2, accountId);
                assertEquals(1, statement.executeUpdate());
            }

            List<TransactionView> history = dao.history(accountId);
            assertEquals(2, history.size());
            assertEquals(
                    List.of(firstHistoryId, secondHistoryId),
                    history.stream().map(TransactionView::id).toList());
            assertEquals(accountId, history.get(0).accountId());
            assertEquals("DEPOSIT", history.get(0).type());
            assertEquals(0, new BigDecimal("125").compareTo(history.get(0).amount()));
            assertEquals(0, new BigDecimal("125").compareTo(history.get(0).balanceAfter()));
            assertEquals(0, new BigDecimal("75").compareTo(history.get(1).amount()));
            assertEquals(0, new BigDecimal("200").compareTo(history.get(1).balanceAfter()));
            assertEquals(firstTime.toInstant(), history.get(0).occurredAt());
            assertEquals(secondTime.toInstant(), history.get(1).occurredAt());
            assertEquals(
                    0,
                    new BigDecimal("200")
                            .compareTo(dao.account(accountId).orElseThrow().balance()));
            assertTrue(dao.history(otherAccountId).isEmpty());
            assertFalse(
                    dao.accounts(otherCustomerId).stream()
                            .anyMatch(account -> account.id().equals(accountId)));
        }
    }

    private static void insertHistory(
            Connection connection,
            UUID id,
            UUID accountId,
            String amount,
            String balanceAfter,
            OffsetDateTime occurredAt)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "INSERT INTO phase2.transaction_history "
                                + "(id, account_id, type, amount, balance_after, occurred_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setObject(2, accountId);
            statement.setString(3, "DEPOSIT");
            statement.setBigDecimal(4, new BigDecimal(amount));
            statement.setBigDecimal(5, new BigDecimal(balanceAfter));
            statement.setObject(6, occurredAt);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static TestTransaction openTransaction() throws SQLException {
        String url = requiredEnvironment("DB_URL");
        String user = requiredEnvironment("DB_USER");
        String password = requiredEnvironment("DB_PASSWORD");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("JdbcBankDaoIT requires a PostgreSQL DB_URL.");
        }
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "3");
        properties.setProperty("socketTimeout", "5");
        Connection connection;
        try {
            connection = DriverManager.getConnection(url, properties);
        } catch (SQLException exception) {
            // URL에 자격 증명이 포함됐을 수 있으므로 접속 예외 원문은 출력하지 않는다.
            throw new IllegalStateException(
                    "JdbcBankDaoIT could not connect. Check the local PostgreSQL environment.");
        }
        boolean configured = false;
        try {
            connection.setAutoCommit(false);
            Connection borrowed =
                    (Connection)
                            Proxy.newProxyInstance(
                                    JdbcBankDaoIT.class.getClassLoader(),
                                    new Class<?>[] {Connection.class},
                                    (proxy, method, arguments) -> {
                                        if (method.getName().equals("close")) return null;
                                        try {
                                            return method.invoke(connection, arguments);
                                        } catch (InvocationTargetException exception) {
                                            throw exception.getCause();
                                        }
                                    });
            DataSource source =
                    (DataSource)
                            Proxy.newProxyInstance(
                                    JdbcBankDaoIT.class.getClassLoader(),
                                    new Class<?>[] {DataSource.class},
                                    (proxy, method, arguments) -> {
                                        if (method.getName().equals("getConnection")
                                                && method.getParameterCount() == 0) {
                                            return borrowed;
                                        }
                                        throw new UnsupportedOperationException(
                                                "Only transaction-bound getConnection() is supported.");
                                    });
            configured = true;
            return new TestTransaction(connection, new JdbcBankDao(source));
        } finally {
            if (!configured) connection.close();
        }
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "JdbcBankDaoIT requires "
                            + key
                            + "; run the integration profile against the migrated local PostgreSQL database.");
        }
        return value;
    }

    /** DAO의 close는 빌린 프록시에만 적용하고 테스트가 실제 연결의 rollback/close를 소유한다. */
    private record TestTransaction(Connection connection, JdbcBankDao dao)
            implements AutoCloseable {
        @Override
        public void close() throws SQLException {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }
}
