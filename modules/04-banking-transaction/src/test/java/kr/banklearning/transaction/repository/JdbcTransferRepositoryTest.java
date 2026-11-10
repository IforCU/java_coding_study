package kr.banklearning.transaction.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.transaction.domain.LedgerEntry;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.service.BankProblem;
import org.junit.jupiter.api.Test;

class JdbcTransferRepositoryTest {
    @Test void accountsLockInDatabaseUuidOrderUsingFkCompatibleRowLock() {
        Fixture fixture = new Fixture(); UUID source = UUID.randomUUID(), destination = UUID.randomUUID();
        new JdbcTransferRepository(fixture.source()).lockAccounts(source, destination);
        assertTrue(fixture.sql.contains("WHERE id IN (?, ?) ORDER BY id FOR NO KEY UPDATE"));
        assertEquals(source, fixture.bindings.get(1)); assertEquals(destination, fixture.bindings.get(2));
        assertTrue(fixture.connectionClosed && fixture.statementClosed && fixture.resultClosed);
    }
    @Test void requestReservationBindsPayloadAndUsesUniqueKeyConflict() {
        Fixture fixture = new Fixture(); TransferCommand command = new TransferCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("123"));
        assertTrue(new JdbcTransferRepository(fixture.source()).reserve(command, LocalDate.of(2026,10,25)));
        assertTrue(fixture.sql.contains("ON CONFLICT (request_id) DO NOTHING"));
        assertEquals(command.requestId(), fixture.bindings.get(1)); assertEquals(command.sourceId(), fixture.bindings.get(2));
        assertEquals(command.destinationId(), fixture.bindings.get(3)); assertEquals(command.amount(), fixture.bindings.get(4));
        assertTrue(fixture.connectionClosed && fixture.statementClosed);
    }
    @Test void foreignKeyViolationBecomesUnknownAccountAndClosesResources() {
        Fixture fixture = new Fixture(); fixture.failure = new SQLException("secret FK detail", "23503");
        BankProblem problem = assertThrows(BankProblem.class, () -> new JdbcTransferRepository(fixture.source()).reserve(
                new TransferCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE), LocalDate.of(2026,10,25)));
        assertEquals(404, problem.status()); assertEquals("ACCOUNT_NOT_FOUND", problem.code());
        assertFalse(problem.toString().contains("secret")); assertTrue(fixture.connectionClosed && fixture.statementClosed);
    }
    @Test void ledgerBindsExactAmountsAndTimestampWithoutBuildingSqlFromValues() {
        Fixture fixture = new Fixture(); Instant instant = Instant.parse("2026-10-25T12:00:00Z");
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "DEPOSIT", new BigDecimal("9007199254740993"), new BigDecimal("9007199254740994"), instant);
        new JdbcTransferRepository(fixture.source()).appendLedger(entry);
        assertEquals(entry.amount(), fixture.bindings.get(5)); assertEquals(entry.balanceAfter(), fixture.bindings.get(6));
        assertEquals(java.sql.Timestamp.from(instant), fixture.bindings.get(7));
        assertFalse(fixture.sql.contains(entry.amount().toPlainString()));
    }
    private static final class Fixture {
        final Map<Integer,Object> bindings = new HashMap<>();
        String sql; SQLException failure;
        boolean connectionClosed, statementClosed, resultClosed;
        DataSource source() {
            return proxy(DataSource.class, (name,args) -> name.equals("getConnection") ? connection() : null);
        }
        Connection connection() {
            return proxy(Connection.class, (name,args) -> switch(name) {
                case "prepareStatement" -> { sql = (String) args[0]; yield statement(); }
                case "close" -> { connectionClosed = true; yield null; }
                case "getMetaData" -> proxy(DatabaseMetaData.class, (method, values) -> method.equals("getDatabaseProductName") ? "PostgreSQL" : null);
                default -> null;
            });
        }
        PreparedStatement statement() {
            return proxy(PreparedStatement.class, (name,args) -> {
                if (name.equals("close")) statementClosed = true;
                if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) bindings.put((int)args[0], args[1]);
                if (name.equals("executeUpdate")) { if (failure != null) throw failure; return 1; }
                if (name.equals("executeQuery")) return proxy(ResultSet.class, (method, values) -> { if (method.equals("close")) resultClosed = true; return null; });
                return null;
            });
        }
    }
    @FunctionalInterface private interface Operation { Object run(String name, Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, Operation operation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object,method,args) -> {
            Object value = operation.run(method.getName(),args); if (value != null) return value;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            return null;
        });
    }
}
