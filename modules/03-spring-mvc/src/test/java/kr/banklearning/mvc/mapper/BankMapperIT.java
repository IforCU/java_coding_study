package kr.banklearning.mvc.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.mvc.dto.BankViews.AccountView;
import kr.banklearning.mvc.dto.BankViews.CustomerView;
import kr.banklearning.mvc.dto.BankViews.TransactionView;
import kr.banklearning.mvc.service.BankProblem;
import kr.banklearning.mvc.service.BankService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

/** PostgreSQL opt-in 검증. Spring proxy/HTTP 검증과 구분하며 기존 데이터는 변경하지 않는다. */
class BankMapperIT {
    @Test
    void mapsCrudAndRejectsNewAccountsForSuspendedCustomers() throws Exception {
        try (TestTransaction transaction = openTransaction()) {
            BankMapper mapper = transaction.mapper();
            BankService service = new BankService(mapper);
            String name = "가상 고객 ' OR '1'='1 한글";
            CustomerView customer = service.createCustomer(name);
            assertEquals(customer, mapper.customer(customer.id()));
            assertEquals(
                    List.of(customer),
                    mapper.customers().stream()
                            .filter(value -> value.id().equals(customer.id()))
                            .toList());

            AccountView account = service.createAccount(customer.id());
            assertEquals(account.id(), mapper.account(account.id()).id());
            assertEquals(customer.id(), mapper.account(account.id()).customerId());
            assertEquals(0, BigDecimal.ZERO.compareTo(mapper.account(account.id()).balance()));
            assertEquals(
                    List.of(account.id()),
                    mapper.accounts(customer.id()).stream().map(AccountView::id).toList());

            service.changeStatus(customer.id(), "SUSPENDED");
            assertEquals("SUSPENDED", mapper.customer(customer.id()).status());
            BankProblem rejected =
                    assertThrows(BankProblem.class, () -> service.createAccount(customer.id()));
            assertEquals(409, rejected.status());
            assertEquals(1, mapper.accounts(customer.id()).size());
            assertEquals(account.id(), service.accounts(customer.id()).get(0).id());

            service.changeStatus(customer.id(), "ACTIVE");
            service.createAccount(customer.id());
            assertEquals(2, mapper.accounts(customer.id()).size());
            assertTrue(mapper.ready());
        }
    }

    @Test
    void appliesOptionalPeriodBoundsAsHalfOpenSeoulBusinessDays() throws Exception {
        try (TestTransaction transaction = openTransaction()) {
            BankMapper mapper = transaction.mapper();
            BankService service = new BankService(mapper);
            CustomerView customer = service.createCustomer("가상 기간 검색 고객");
            AccountView account = service.createAccount(customer.id());
            AccountView otherAccount = service.createAccount(customer.id());
            OffsetDateTime from = OffsetDateTime.parse("2026-10-25T00:00:00+09:00");
            OffsetDateTime until = from.plusDays(1);
            List<String> ids = List.of(uuid(), uuid(), uuid(), uuid());

            insertHistory(transaction.connection(), ids.get(3), account.id(), "40", until);
            insertHistory(transaction.connection(), ids.get(1), account.id(), "20", from);
            insertHistory(
                    transaction.connection(),
                    ids.get(0),
                    account.id(),
                    "10",
                    from.minusNanos(1000));
            insertHistory(
                    transaction.connection(),
                    ids.get(2),
                    account.id(),
                    "30",
                    until.minusNanos(1000));
            insertHistory(transaction.connection(), uuid(), otherAccount.id(), "10", from);
            setBalance(transaction.connection(), account.id(), "40");
            setBalance(transaction.connection(), otherAccount.id(), "10");
            // 직접 SQL로 준비한 뒤 MyBatis 캐시가 이전 데이터를 가리지 않게 한다.
            transaction.session().clearCache();

            List<TransactionView> selected =
                    service.history(account.id(), "2026-10-25", "2026-10-25");
            assertEquals(
                    List.of(ids.get(1), ids.get(2)),
                    selected.stream().map(TransactionView::id).toList());
            assertEquals(account.id(), selected.get(0).accountId());
            assertEquals("DEPOSIT", selected.get(0).type());
            assertEquals(0, BigDecimal.TEN.compareTo(selected.get(0).amount()));
            assertEquals(0, new BigDecimal("20").compareTo(selected.get(0).balanceAfter()));
            assertEquals(from.toInstant(), selected.get(0).occurredAt().toInstant());
            assertEquals(
                    until.minusNanos(1000).toInstant(), selected.get(1).occurredAt().toInstant());
            assertEquals(
                    List.of(ids.get(1), ids.get(2), ids.get(3)),
                    mapper.history(account.id(), from, null).stream()
                            .map(TransactionView::id)
                            .toList());
            assertEquals(
                    List.of(ids.get(0), ids.get(1), ids.get(2)),
                    mapper.history(account.id(), null, until).stream()
                            .map(TransactionView::id)
                            .toList());
            assertEquals(
                    ids,
                    mapper.history(account.id(), null, null).stream()
                            .map(TransactionView::id)
                            .toList());
            assertTrue(service.history(account.id(), "2026-10-27", "2026-10-27").isEmpty());
        }
    }

    private static void insertHistory(
            Connection connection,
            String id,
            String accountId,
            String balanceAfter,
            OffsetDateTime occurredAt)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "INSERT INTO phase3.transaction_history "
                                + "(id, account_id, type, amount, balance_after, occurred_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, UUID.fromString(id));
            statement.setObject(2, UUID.fromString(accountId));
            statement.setString(3, "DEPOSIT");
            statement.setBigDecimal(4, BigDecimal.TEN);
            statement.setBigDecimal(5, new BigDecimal(balanceAfter));
            statement.setObject(6, occurredAt);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void setBalance(Connection connection, String accountId, String balance)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("UPDATE phase3.account SET balance = ? WHERE id = ?")) {
            statement.setBigDecimal(1, new BigDecimal(balance));
            statement.setObject(2, UUID.fromString(accountId));
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static TestTransaction openTransaction() throws Exception {
        String url = requiredEnvironment("DB_URL");
        String user = requiredEnvironment("DB_USER");
        String password = requiredEnvironment("DB_PASSWORD");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("BankMapperIT requires a PostgreSQL DB_URL.");
        }
        Properties credentials = new Properties();
        credentials.setProperty("user", user);
        credentials.setProperty("password", password);
        credentials.setProperty("connectTimeout", "3");
        credentials.setProperty("socketTimeout", "5");
        Connection connection;
        try {
            connection = DriverManager.getConnection(url, credentials);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "BankMapperIT could not connect. Check the local PostgreSQL environment.");
        }
        boolean configured = false;
        try {
            connection.setAutoCommit(false);
            Connection borrowed =
                    (Connection)
                            Proxy.newProxyInstance(
                                    BankMapperIT.class.getClassLoader(),
                                    new Class<?>[] {Connection.class},
                                    (proxy, method, arguments) -> {
                                        if (method.getName().equals("close")) return null;
                                        if (method.getName().equals("commit")
                                                || (method.getName().equals("setAutoCommit")
                                                        && Boolean.TRUE.equals(arguments[0]))) {
                                            throw new SQLException(
                                                    "The integration test connection must never commit.");
                                        }
                                        try {
                                            return method.invoke(connection, arguments);
                                        } catch (InvocationTargetException exception) {
                                            throw exception.getCause();
                                        }
                                    });
            DataSource source =
                    (DataSource)
                            Proxy.newProxyInstance(
                                    BankMapperIT.class.getClassLoader(),
                                    new Class<?>[] {DataSource.class},
                                    (proxy, method, arguments) -> {
                                        if (method.getName().equals("getConnection")
                                                && method.getParameterCount() == 0) return borrowed;
                                        throw new UnsupportedOperationException(
                                                "Only transaction-bound getConnection() is supported.");
                                    });
            JdbcTransactionFactory transactions = new JdbcTransactionFactory();
            Properties settings = new Properties();
            // MyBatis가 연결 반환 과정에서 autoCommit=true로 바꾸어 준비 데이터를 커밋하지 않게 한다.
            settings.setProperty("skipSetAutoCommitOnClose", "true");
            transactions.setProperties(settings);
            Configuration configuration =
                    new Configuration(new Environment("phase3-it", transactions, source));
            configuration.setDefaultStatementTimeout(3);
            String resource = "kr/banklearning/mvc/mapper/BankMapper.xml";
            try (InputStream input = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(
                                input, configuration, resource, configuration.getSqlFragments())
                        .parse();
            }
            SqlSession session =
                    new SqlSessionFactoryBuilder().build(configuration).openSession(false);
            TestTransaction result =
                    new TestTransaction(connection, session, session.getMapper(BankMapper.class));
            configured = true;
            return result;
        } finally {
            if (!configured) {
                try {
                    connection.rollback();
                } finally {
                    connection.close();
                }
            }
        }
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "BankMapperIT requires "
                            + key
                            + "; use the integration profile with the migrated local PostgreSQL database.");
        }
        return value;
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    private record TestTransaction(Connection connection, SqlSession session, BankMapper mapper)
            implements AutoCloseable {
        @Override
        public void close() throws SQLException {
            try {
                session.close();
            } finally {
                try {
                    connection.rollback();
                } finally {
                    connection.close();
                }
            }
        }
    }
}
