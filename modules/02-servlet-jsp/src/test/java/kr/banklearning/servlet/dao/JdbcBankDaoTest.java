package kr.banklearning.servlet.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import org.junit.jupiter.api.Test;

class JdbcBankDaoTest {
    @Test
    void customerNameIsBoundAsValueWithoutChangingSql() {
        Fixture fixture = new Fixture();
        String text = "학습'; DROP TABLE phase2.customer; --";
        UUID id = UUID.randomUUID();
        new JdbcBankDao(fixture.source()).insertCustomer(new CustomerView(id, text, "ACTIVE"));
        assertEquals(
                "INSERT INTO phase2.customer (id, name, status) VALUES (?, ?, ?)", fixture.sql);
        assertEquals(id, fixture.bindings.get(1));
        assertEquals(text, fixture.bindings.get(2));
        assertFalse(fixture.sql.contains(text));
        assertEquals(3, fixture.timeout);
        assertTrue(fixture.connectionClosed && fixture.statementClosed);
    }

    @Test
    void accountOwnerAndBalanceAreTypedBindings() {
        Fixture fixture = new Fixture();
        AccountView account =
                new AccountView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("1234567890123456789"));
        new JdbcBankDao(fixture.source()).insertAccount(account);
        assertEquals(account.customerId(), fixture.bindings.get(2));
        assertEquals(account.balance(), fixture.bindings.get(3));
        assertTrue(fixture.connectionClosed && fixture.statementClosed);
    }

    @Test
    void accountMappingPreservesUuidAndDecimalAndClosesEveryResource() {
        Fixture fixture = new Fixture();
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        fixture.row.putAll(
                Map.of(
                        "id",
                        id,
                        "customer_id",
                        owner,
                        "balance",
                        new BigDecimal("9007199254740993")));
        var results = new JdbcBankDao(fixture.source()).accounts(owner);
        assertEquals(owner, fixture.bindings.get(1));
        assertEquals(id, results.get(0).id());
        assertEquals(new BigDecimal("9007199254740993"), results.get(0).balance());
        assertTrue(fixture.connectionClosed && fixture.statementClosed && fixture.resultClosed);
        assertThrows(UnsupportedOperationException.class, results::clear);
    }

    @Test
    void historyMapsOffsetTimestampToInstantAndBindsOnlyRequestedAccount() {
        Fixture fixture = new Fixture();
        UUID account = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-10-25T21:00:00+09:00");
        fixture.row.putAll(
                Map.of(
                        "id",
                        UUID.randomUUID(),
                        "account_id",
                        account,
                        "type",
                        "DEPOSIT",
                        "amount",
                        BigDecimal.TEN,
                        "balance_after",
                        BigDecimal.TEN,
                        "occurred_at",
                        timestamp));
        var result = new JdbcBankDao(fixture.source()).history(account).get(0);
        assertEquals(timestamp.toInstant(), result.occurredAt());
        assertEquals(account, fixture.bindings.get(1));
        assertTrue(fixture.sql.contains("WHERE account_id = ? ORDER BY occurred_at, id"));
    }

    @Test
    void executeFailureClosesAcquiredResourcesAndHidesSqlDetails() {
        Fixture fixture = new Fixture();
        fixture.failExecute = true;
        DataAccessException exception =
                assertThrows(
                        DataAccessException.class,
                        () -> new JdbcBankDao(fixture.source()).customers());
        assertTrue(fixture.connectionClosed && fixture.statementClosed);
        assertFalse(exception.getMessage().contains("secret"));
        assertNull(exception.getCause());
    }

    @Test
    void mappingFailureStillClosesResultSetStatementAndConnection() {
        Fixture fixture = new Fixture();
        fixture.failMapping = true;
        assertThrows(
                DataAccessException.class, () -> new JdbcBankDao(fixture.source()).customers());
        assertTrue(fixture.resultClosed && fixture.statementClosed && fixture.connectionClosed);
    }

    @Test
    void unavailableDatabaseIsNotReadyAndDoesNotLeakInternalMessage() {
        Fixture fixture = new Fixture();
        fixture.failConnect = true;
        assertFalse(new JdbcBankDao(fixture.source()).ready());
        DataAccessException exception =
                assertThrows(
                        DataAccessException.class,
                        () -> new JdbcBankDao(fixture.source()).customers());
        assertFalse(exception.toString().contains("secret"));
    }

    @Test
    void readinessQueriesAllRequiredSchemaTables() {
        Fixture fixture = new Fixture();
        fixture.row.put("customers", 1L);
        assertTrue(new JdbcBankDao(fixture.source()).ready());
        assertTrue(fixture.sql.contains("phase2.customer"));
        assertTrue(fixture.sql.contains("phase2.account"));
        assertTrue(fixture.sql.contains("phase2.transaction_history"));
    }

    private static final class Fixture {
        final Map<Integer, Object> bindings = new HashMap<>();
        final Map<String, Object> row = new HashMap<>();
        String sql;
        int timeout;
        boolean connectionClosed,
                statementClosed,
                resultClosed,
                failExecute,
                failMapping,
                failConnect;

        DataSource source() {
            return (DataSource)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {DataSource.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("getConnection")) {
                                    if (failConnect)
                                        throw new SQLException("secret connection URL");
                                    return connection();
                                }
                                return fallback(method.getReturnType());
                            });
        }

        Connection connection() {
            return (Connection)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {Connection.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("close")) connectionClosed = true;
                                if (method.getName().equals("prepareStatement")) {
                                    sql = (String) args[0];
                                    return statement();
                                }
                                return fallback(method.getReturnType());
                            });
        }

        PreparedStatement statement() {
            return (PreparedStatement)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {PreparedStatement.class},
                            (proxy, method, args) -> {
                                String name = method.getName();
                                if (name.equals("close")) statementClosed = true;
                                if (name.equals("setQueryTimeout")) timeout = (int) args[0];
                                if (name.equals("setObject")
                                        || name.equals("setString")
                                        || name.equals("setBigDecimal"))
                                    bindings.put((int) args[0], args[1]);
                                if (name.startsWith("execute")) {
                                    if (failExecute) throw new SQLException("secret SQL password");
                                    return name.equals("executeUpdate") ? 1 : result();
                                }
                                return fallback(method.getReturnType());
                            });
        }

        ResultSet result() {
            int[] cursor = {0};
            return (ResultSet)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {ResultSet.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("close")) resultClosed = true;
                                if (method.getName().equals("next")) return cursor[0]++ == 0;
                                if (method.getName().startsWith("get")) {
                                    if (failMapping) throw new SQLException("secret mapping error");
                                    return row.get((String) args[0]);
                                }
                                return fallback(method.getReturnType());
                            });
        }

        static Object fallback(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
