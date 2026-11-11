package kr.banklearning.bootstrap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DatabaseProbe {
    private final ConnectionFactory connections;
    private final int timeoutSeconds;

    public DatabaseProbe(ConnectionFactory connections, int timeoutSeconds) {
        this.connections = connections;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void verify() throws SQLException {
        try (Connection connection = connections.open()) {
            connection.setReadOnly(true);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "SELECT schema_version FROM bootstrap.app_metadata WHERE environment = ?")) {
                statement.setString(1, "local-learning");
                statement.setQueryTimeout(timeoutSeconds);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getInt("schema_version") != 1) {
                        throw new SQLException("Bootstrap schema is not ready");
                    }
                }
            }
        }
    }
}
