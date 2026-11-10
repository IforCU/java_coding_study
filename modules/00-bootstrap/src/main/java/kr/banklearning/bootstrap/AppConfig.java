package kr.banklearning.bootstrap;

import java.util.Map;

/** 환경 설정을 시작 시 검증한다. 인증정보가 toString에 노출되지 않게 일반 클래스를 사용한다. */
public final class AppConfig {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int timeoutSeconds;

    private AppConfig(String dbUrl, String dbUser, String dbPassword, int timeoutSeconds) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static AppConfig from(Map<String, String> environment) {
        String url = required(environment, "DB_URL");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("DB_URL must be a PostgreSQL JDBC URL");
        }
        int timeout;
        try {
            timeout = Integer.parseInt(environment.getOrDefault("DB_TIMEOUT_SECONDS", "3"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("DB_TIMEOUT_SECONDS must be an integer");
        }
        if (timeout < 1 || timeout > 30) {
            throw new IllegalArgumentException("DB_TIMEOUT_SECONDS must be between 1 and 30");
        }
        return new AppConfig(url, required(environment, "DB_USER"),
                required(environment, "DB_PASSWORD"), timeout);
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    public String dbUrl() { return dbUrl; }
    public String dbUser() { return dbUser; }
    public String dbPassword() { return dbPassword; }
    public int timeoutSeconds() { return timeoutSeconds; }
}
