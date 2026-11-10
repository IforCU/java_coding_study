package kr.banklearning.operations.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionTrackingMode;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Set;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public final class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override protected Class<?>[] getRootConfigClasses() { return new Class<?>[]{AppConfig.class, SecurityConfig.class}; }
    @Override protected Class<?>[] getServletConfigClasses() { return new Class<?>[]{WebConfig.class}; }
    @Override protected String[] getServletMappings() { return new String[]{"/"}; }
    @Override public void onStartup(ServletContext context) throws ServletException {
        super.onStartup(context); context.getSessionCookieConfig().setHttpOnly(true); context.setSessionTimeout(15); context.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
        context.addListener(new jakarta.servlet.ServletContextListener() {
            @Override public void contextDestroyed(jakarta.servlet.ServletContextEvent event) {
                Enumeration<Driver> drivers = DriverManager.getDrivers();
                while (drivers.hasMoreElements()) {
                    Driver driver = drivers.nextElement(); if (driver.getClass().getClassLoader() == WebInitializer.class.getClassLoader()) {
                        try { DriverManager.deregisterDriver(driver); } catch (SQLException exception) { /* 비밀정보를 출력하지 않는다. */ }
                    }
                }
            }
        });
    }
}
