package kr.banklearning.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;

public final class JdbcConnectionFactory implements ConnectionFactory, AutoCloseable {
    private final PGSimpleDataSource dataSource;

    public JdbcConnectionFactory(AppConfig config) {
        // Tomcat의 부모 class loader가 먼저 JDBC 검색을 끝내도 WAR의 드라이버를 초기화한다.
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(config.dbUrl());
        dataSource.setUser(config.dbUser());
        dataSource.setPassword(config.dbPassword());
        dataSource.setConnectTimeout(config.timeoutSeconds());
        dataSource.setSocketTimeout(config.timeoutSeconds());
        dataSource.setApplicationName("bank-learning-phase0");
    }

    @Override
    public Connection open() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() throws SQLException {
        // 현재 WAR가 등록한 드라이버를 내려서 재배포 시 class loader가 남지 않게 한다.
        if (Driver.isRegistered()) {
            Driver.deregister();
        }
    }
}
