package kr.banklearning.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JdbcConnectionFactoryTest {
    @Test
    void registersWarLocalDriverAfterContainerDiscoveryAndDeregistersItOnShutdown()
            throws Exception {
        // Tomcat은 WAR를 읽기 전에 부모 클래스 로더에서 JDBC 드라이버 검색을 끝낸다.
        DriverManager.getDrivers();
        URL productionClasses =
                JdbcConnectionFactory.class.getProtectionDomain().getCodeSource().getLocation();
        URL testClasses =
                JdbcConnectionFactoryTest.class.getProtectionDomain().getCodeSource().getLocation();
        URL driverJar =
                org.postgresql.Driver.class.getProtectionDomain().getCodeSource().getLocation();

        try (URLClassLoader webApplication =
                new URLClassLoader(
                        new URL[] {productionClasses, testClasses, driverJar},
                        ClassLoader.getPlatformClassLoader())) {
            Class<?> configType = webApplication.loadClass(AppConfig.class.getName());
            Object config =
                    configType
                            .getMethod("from", Map.class)
                            .invoke(
                                    null,
                                    Map.of(
                                            "DB_URL", "jdbc:postgresql://db:5432/bank_learning",
                                            "DB_USER", "local-test-user",
                                            "DB_PASSWORD", "local-test-password"));
            Class<?> factoryType = webApplication.loadClass(JdbcConnectionFactory.class.getName());
            AutoCloseable factory =
                    (AutoCloseable) factoryType.getConstructor(configType).newInstance(config);
            try {
                Class<?> inspection = webApplication.loadClass(DriverVisibility.class.getName());
                Method registeredDriverLoader =
                        inspection.getMethod("registeredDriverLoader", String.class);

                // getDriver는 접속하지 않는다. isRegistered를 먼저 호출하면 드라이버가 늦게
                // 초기화되어 원래 자동 검색 누락 문제가 가려지므로 실제 검색을 먼저 검증한다.
                Object visibleDriverLoader =
                        registeredDriverLoader.invoke(
                                null, "jdbc:postgresql://db:5432/bank_learning");
                assertSame(webApplication, visibleDriverLoader);
                Class<?> driverType = webApplication.loadClass("org.postgresql.Driver");
                assertSame(webApplication, driverType.getClassLoader());
                assertTrue((Boolean) driverType.getMethod("isRegistered").invoke(null));

                factory.close();

                assertFalse((Boolean) driverType.getMethod("isRegistered").invoke(null));
                InvocationTargetException failure =
                        assertThrows(
                                InvocationTargetException.class,
                                () ->
                                        registeredDriverLoader.invoke(
                                                null, "jdbc:postgresql://db:5432/bank_learning"));
                assertInstanceOf(SQLException.class, failure.getCause());
            } finally {
                // 단언 실패 시에도 WAR의 전역 DriverManager 등록을 남기지 않는다.
                factory.close();
            }
        }
    }

    /** 호출자의 클래스 로더가 WAR일 때 DriverManager가 찾는 드라이버를 확인한다. */
    public static final class DriverVisibility {
        public static ClassLoader registeredDriverLoader(String url) throws SQLException {
            return DriverManager.getDriver(url).getClass().getClassLoader();
        }
    }
}
