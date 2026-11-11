package kr.banklearning.servlet.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import kr.banklearning.servlet.dao.JdbcBankDao;
import kr.banklearning.servlet.service.BankService;

@WebListener
public final class AppListener implements ServletContextListener {
    public static final String SERVICE = BankService.class.getName();
    private HikariDataSource pool;

    @Override
    public void contextInitialized(ServletContextEvent event) {
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
            config.setPoolName("phase2-pool");
            pool = new HikariDataSource(config);
            event.getServletContext().setAttribute(SERVICE, new BankService(new JdbcBankDao(pool)));
        } catch (RuntimeException exception) {
            close();
            throw new IllegalStateException("학습용 DB 환경변수를 확인하세요.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(SERVICE);
        close();
    }

    private void close() {
        if (pool != null) pool.close();
        ClassLoader owner = getClass().getClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == owner) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException exception) {
                    /* 종료 과정에서 비밀정보를 기록하지 않는다. */
                }
            }
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("필수 환경변수가 없습니다.");
        return value;
    }
}
