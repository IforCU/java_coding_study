package kr.banklearning.bootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseProbeTest {
    @Test
    void verifiesExpectedSchemaUsingReadOnlyBoundQueryAndClosesAllResources() throws SQLException {
        JdbcStub jdbc = new JdbcStub();

        new DatabaseProbe(jdbc, 7).verify();

        assertAll(
                () -> assertEquals(1, jdbc.openCount),
                () -> assertEquals(Boolean.TRUE, jdbc.readOnly),
                () -> assertEquals("SELECT schema_version FROM bootstrap.app_metadata WHERE environment = ?",
                        jdbc.sql),
                () -> assertFalse(jdbc.sql.contains("local-learning")),
                () -> assertEquals(1, jdbc.parameterIndex),
                () -> assertEquals("local-learning", jdbc.parameterValue),
                () -> assertEquals(7, jdbc.queryTimeout),
                () -> assertEquals("schema_version", jdbc.versionColumn),
                () -> assertEquals(1, jdbc.executeCount),
                () -> assertEquals(List.of("result", "statement", "connection"), jdbc.closed));
    }

    @Test
    void missingMetadataFailsReadinessAndStillClosesAllResources() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.rowPresent = false;

        SQLException failure = assertThrows(SQLException.class,
                () -> new DatabaseProbe(jdbc, 3).verify());

        assertAll(
                () -> assertEquals("Bootstrap schema is not ready", failure.getMessage()),
                () -> assertEquals(0, jdbc.versionReads),
                () -> assertEquals(List.of("result", "statement", "connection"), jdbc.closed));
    }

    @Test
    void unexpectedSchemaVersionsFailReadinessAndCloseAllResources() {
        for (int version : List.of(0, 2, -1)) {
            JdbcStub jdbc = new JdbcStub();
            jdbc.schemaVersion = version;

            SQLException failure = assertThrows(SQLException.class,
                    () -> new DatabaseProbe(jdbc, 3).verify(), "schema version=" + version);

            assertEquals("Bootstrap schema is not ready", failure.getMessage());
            assertEquals(List.of("result", "statement", "connection"), jdbc.closed);
        }
    }

    @Test
    void connectionFailureIsPropagatedWithoutClosingResourcesThatWereNotOpened() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.openFailure = new SQLException("connection unavailable");

        SQLException failure = assertThrows(SQLException.class,
                () -> new DatabaseProbe(jdbc, 3).verify());

        assertSame(jdbc.openFailure, failure);
        assertEquals(1, jdbc.openCount);
        assertEquals(List.of(), jdbc.closed);
    }

    @Test
    void preparingQueryFailureClosesTheOpenedConnection() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.prepareFailure = new SQLException("relation unavailable");

        SQLException failure = assertThrows(SQLException.class,
                () -> new DatabaseProbe(jdbc, 3).verify());

        assertSame(jdbc.prepareFailure, failure);
        assertEquals(List.of("connection"), jdbc.closed);
        assertEquals(0, jdbc.executeCount);
    }

    @Test
    void executingQueryFailureClosesStatementAndConnection() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.executeFailure = new SQLException("query timeout");

        SQLException failure = assertThrows(SQLException.class,
                () -> new DatabaseProbe(jdbc, 3).verify());

        assertSame(jdbc.executeFailure, failure);
        assertEquals(List.of("statement", "connection"), jdbc.closed);
        assertEquals(1, jdbc.executeCount);
    }

    @Test
    void readingResultFailureClosesResultStatementAndConnection() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.readFailure = new SQLException("result unavailable");

        SQLException failure = assertThrows(SQLException.class,
                () -> new DatabaseProbe(jdbc, 3).verify());

        assertSame(jdbc.readFailure, failure);
        assertEquals(List.of("result", "statement", "connection"), jdbc.closed);
        assertEquals(1, jdbc.versionReads);
    }

    /** 호출을 기록하고 예상하지 않은 JDBC API 사용을 즉시 실패시키는 스텁이다. */
    private static final class JdbcStub implements ConnectionFactory {
        private final List<String> closed = new ArrayList<>();
        private int openCount;
        private Boolean readOnly;
        private String sql;
        private int parameterIndex;
        private String parameterValue;
        private int queryTimeout;
        private int executeCount;
        private String versionColumn;
        private int versionReads;
        private boolean rowPresent = true;
        private int schemaVersion = 1;
        private SQLException openFailure;
        private SQLException prepareFailure;
        private SQLException executeFailure;
        private SQLException readFailure;

        private final ResultSet result = proxy(ResultSet.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "next" -> rowPresent;
                case "getInt" -> {
                    versionColumn = (String) args[0];
                    versionReads++;
                    if (readFailure != null) {
                        throw readFailure;
                    }
                    yield schemaVersion;
                }
                case "close" -> { closed.add("result"); yield null; }
                default -> throw new AssertionError("Unexpected ResultSet call: " + method.getName());
            };
        });

        private final PreparedStatement statement = proxy(PreparedStatement.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "setString" -> {
                    parameterIndex = (Integer) args[0];
                    parameterValue = (String) args[1];
                    yield null;
                }
                case "setQueryTimeout" -> { queryTimeout = (Integer) args[0]; yield null; }
                case "executeQuery" -> {
                    executeCount++;
                    if (executeFailure != null) {
                        throw executeFailure;
                    }
                    yield result;
                }
                case "close" -> { closed.add("statement"); yield null; }
                default -> throw new AssertionError("Unexpected PreparedStatement call: " + method.getName());
            };
        });

        private final Connection connection = proxy(Connection.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "setReadOnly" -> { readOnly = (Boolean) args[0]; yield null; }
                case "prepareStatement" -> {
                    sql = (String) args[0];
                    if (prepareFailure != null) {
                        throw prepareFailure;
                    }
                    yield statement;
                }
                case "close" -> { closed.add("connection"); yield null; }
                default -> throw new AssertionError("Unexpected Connection call: " + method.getName());
            };
        });

        @Override
        public Connection open() throws SQLException {
            openCount++;
            if (openFailure != null) {
                throw openFailure;
            }
            return connection;
        }

        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
        }
    }
}
