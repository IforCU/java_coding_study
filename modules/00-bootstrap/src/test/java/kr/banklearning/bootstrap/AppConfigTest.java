package kr.banklearning.bootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    private static final String URL = "jdbc:postgresql://db:5432/bank_learning";
    private static final String USER = "test-user-canary";
    private static final String PASSWORD = "test-password-canary";

    @Test
    void loadsRequiredSettingsAndUsesDefaultTimeout() {
        AppConfig config = AppConfig.from(validEnvironment());

        assertAll(
                () -> assertEquals(URL, config.dbUrl()),
                () -> assertEquals(USER, config.dbUser()),
                () -> assertEquals(PASSWORD, config.dbPassword()),
                () -> assertEquals(3, config.timeoutSeconds()));
    }

    @Test
    void rejectsMissingOrBlankUrlWithoutDisclosingOtherSettings() {
        Map<String, String> missing = validEnvironment();
        missing.remove("DB_URL");
        assertRequiredFailure(missing, "DB_URL");

        for (String blank : List.of("", " ", "\t\r\n")) {
            Map<String, String> environment = validEnvironment();
            environment.put("DB_URL", blank);
            assertRequiredFailure(environment, "DB_URL");
        }
    }

    @Test
    void rejectsNonPostgresqlUrlWithoutEchoingCredentials() {
        Map<String, String> environment = validEnvironment();
        String invalidUrl = "jdbc:mysql://db/bank?user=" + USER + "&password=" + PASSWORD;
        environment.put("DB_URL", invalidUrl);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.from(environment));

        assertEquals("DB_URL must be a PostgreSQL JDBC URL", failure.getMessage());
        assertNoSecrets(failure.toString());
        assertFalse(failure.toString().contains(invalidUrl));
    }

    @Test
    void rejectsMissingOrBlankUserAndPassword() {
        for (String key : List.of("DB_USER", "DB_PASSWORD")) {
            Map<String, String> missing = validEnvironment();
            missing.remove(key);
            assertRequiredFailure(missing, key);

            for (String blank : List.of("", " ", "\t\r\n")) {
                Map<String, String> environment = validEnvironment();
                environment.put(key, blank);
                assertRequiredFailure(environment, key);
            }
        }
    }

    @Test
    void acceptsBothTimeoutBoundariesAndPreservesPasswordExactly() {
        for (String timeout : List.of("1", "30")) {
            Map<String, String> environment = validEnvironment();
            environment.put("DB_TIMEOUT_SECONDS", timeout);
            environment.put("DB_PASSWORD", "  " + PASSWORD + "  ");

            AppConfig config = AppConfig.from(environment);

            assertEquals(Integer.parseInt(timeout), config.timeoutSeconds());
            assertEquals("  " + PASSWORD + "  ", config.dbPassword());
        }
    }

    @Test
    void rejectsTimeoutOutsideOneToThirtySeconds() {
        for (String timeout : List.of("0", "-1", "31", "2147483647")) {
            Map<String, String> environment = validEnvironment();
            environment.put("DB_TIMEOUT_SECONDS", timeout);

            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> AppConfig.from(environment), "timeout=" + timeout);

            assertEquals("DB_TIMEOUT_SECONDS must be between 1 and 30", failure.getMessage());
            assertNoSecrets(failure.toString());
        }
    }

    @Test
    void rejectsMalformedTimeoutWithoutEchoingItsValue() {
        for (String timeout : List.of("", " ", "1.5", "2147483648", PASSWORD)) {
            Map<String, String> environment = validEnvironment();
            environment.put("DB_TIMEOUT_SECONDS", timeout);

            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> AppConfig.from(environment));

            assertEquals("DB_TIMEOUT_SECONDS must be an integer", failure.getMessage());
            assertNoSecrets(failure.toString());
        }
    }

    @Test
    void objectDescriptionDoesNotExposeUrlUserOrPassword() {
        Map<String, String> environment = validEnvironment();
        String credentialUrl = URL + "?user=" + USER + "&password=" + PASSWORD;
        environment.put("DB_URL", credentialUrl);

        String description = AppConfig.from(environment).toString();

        assertNoSecrets(description);
        assertFalse(description.contains(credentialUrl));
        assertFalse(description.contains(URL));
    }

    private static Map<String, String> validEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("DB_URL", URL);
        environment.put("DB_USER", USER);
        environment.put("DB_PASSWORD", PASSWORD);
        return environment;
    }

    private static void assertRequiredFailure(Map<String, String> environment, String key) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.from(environment));
        assertEquals(key + " is required", failure.getMessage());
        assertNoSecrets(failure.toString());
    }

    private static void assertNoSecrets(String text) {
        assertAll(
                () -> assertFalse(text.contains(USER)),
                () -> assertFalse(text.contains(PASSWORD)));
    }
}
