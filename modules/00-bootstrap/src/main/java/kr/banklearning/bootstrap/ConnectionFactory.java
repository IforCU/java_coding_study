package kr.banklearning.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFactory {
    Connection open() throws SQLException;
}
