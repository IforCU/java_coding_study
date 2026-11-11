package kr.banklearning.bootstrap;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(
        urlPatterns = {"/health/live", "/health/ready"},
        loadOnStartup = 1)
public final class BootstrapServlet extends HttpServlet {
    private transient DatabaseProbe database;
    private transient JdbcConnectionFactory connections;

    @Override
    public void init() throws ServletException {
        try {
            AppConfig config = AppConfig.from(System.getenv());
            connections = new JdbcConnectionFactory(config);
            database = new DatabaseProbe(connections, config.timeoutSeconds());
        } catch (IllegalArgumentException exception) {
            throw new ServletException("Invalid database environment configuration", exception);
        }
    }

    @Override
    public void destroy() {
        if (connections != null) {
            try {
                connections.close();
            } catch (SQLException exception) {
                log("Could not deregister the application JDBC driver during shutdown.");
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if ("/health/live".equals(request.getServletPath())) {
            response.getWriter().write("{\"status\":\"UP\"}");
            return;
        }
        try {
            database.verify();
            response.getWriter()
                    .write(
                            "{\"status\":\"UP\",\"database\":\"UP\","
                                    + "\"environment\":\"local-learning\",\"schemaVersion\":1}");
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"status\":\"DOWN\",\"database\":\"DOWN\"}");
            log("Readiness failed: database unavailable or bootstrap schema not ready.");
        }
    }
}
