package kr.banklearning.mvc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class PoolFactory {
    private PoolFactory() {}

    public static HikariDataSource create() {
        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.postgresql.Driver");
            config.setJdbcUrl(env("DB_URL"));
            config.setUsername(env("DB_USER"));
            config.setPassword(env("DB_PASSWORD"));
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(3000);
            config.setValidationTimeout(1000);
            config.setInitializationFailTimeout(-1);
            config.setPoolName("phase3-pool");
            return new HikariDataSource(config);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("학습용 DB 환경변수를 확인하세요.");
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("필수 환경변수가 없습니다.");
        return value;
    }
}
