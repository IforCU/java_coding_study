package kr.banklearning.operations.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTraceFilter.class);
    public static final String ATTRIBUTE = "requestId";
    private static final Set<String> ROUTES =
            Set.of(
                    "/",
                    "/profile",
                    "/profile/note",
                    "/admin/audit",
                    "/health",
                    "/csrf",
                    "/login",
                    "/logout");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader("X-Request-ID");
        String trace =
                supplied != null && supplied.matches("[A-Za-z0-9-]{1,64}")
                        ? supplied
                        : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, trace);
        response.setHeader("X-Request-ID", trace);
        int status = 500;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String route = ROUTES.contains(path) ? path : "/other";
            String method =
                    request.getMethod().matches("GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS")
                            ? request.getMethod()
                            : "OTHER";
            LOG.info(
                    "request method={} path={} status={} requestId={}",
                    method,
                    route,
                    status,
                    trace);
        }
    }

    public static String id(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        return value instanceof String text ? text : "unavailable";
    }
}
