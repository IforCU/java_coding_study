package kr.banklearning.operations.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class JdbcProfileRepositoryTest {
    @Test void noteAndPrincipalAreBoundSeparatelyWithOwnerCondition() {
        Fixture fixture = new Fixture(); String note = "'; UPDATE phase5.profile SET note='bad'; --";
        assertEquals(1, new JdbcProfileRepository(fixture.source()).updateNote("learner", note));
        assertEquals("UPDATE phase5.profile SET note = ? WHERE username = ?", fixture.sql);
        assertEquals(note, fixture.bindings.get(1)); assertEquals("learner", fixture.bindings.get(2));
        assertFalse(fixture.sql.contains(note)); assertTrue(fixture.connectionClosed && fixture.statementClosed);
    }
    @Test void profileSelectionBindsCurrentPrincipalAndClosesEmptyResult() {
        Fixture fixture = new Fixture();
        assertTrue(new JdbcProfileRepository(fixture.source()).findProfile("learner").isEmpty());
        assertTrue(fixture.sql.contains("WHERE username = ?")); assertEquals("learner", fixture.bindings.get(1));
        assertTrue(fixture.connectionClosed && fixture.statementClosed && fixture.resultClosed);
    }
    @Test void sqlFailureClosesResourcesAndStaysInsideDataAccessExceptionBoundary() {
        Fixture fixture = new Fixture(); fixture.fail = true;
        assertThrows(org.springframework.dao.DataAccessException.class, () -> new JdbcProfileRepository(fixture.source()).updateNote("learner", "note"));
        assertTrue(fixture.connectionClosed && fixture.statementClosed);
    }
    private static final class Fixture {
        final Map<Integer,Object> bindings = new HashMap<>();
        String sql; boolean fail, connectionClosed, statementClosed, resultClosed;
        DataSource source() { return proxy(DataSource.class, (name,args) -> name.equals("getConnection") ? connection() : null); }
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
                if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) bindings.put((int) args[0], args[1]);
                if (name.startsWith("execute") && fail) throw new SQLException("secret internal detail", "08006");
                if (name.equals("executeUpdate")) return 1;
                if (name.equals("executeQuery")) return proxy(ResultSet.class, (method, values) -> { if (method.equals("close")) resultClosed = true; return null; });
                return null;
            });
        }
    }
    @FunctionalInterface private interface Operation { Object run(String name,Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, Operation operation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object,method,args) -> {
            Object result = operation.run(method.getName(),args); if (result != null) return result;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            return null;
        });
    }
}
