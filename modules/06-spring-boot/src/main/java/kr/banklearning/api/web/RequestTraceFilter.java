package kr.banklearning.api.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTraceFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader("X-Request-ID");
        String trace = supplied != null && supplied.matches("[A-Za-z0-9-]{1,64}") ? supplied : UUID.randomUUID().toString();
        request.setAttribute("requestId", trace); response.setHeader("X-Request-ID", trace);
        int status = 500;
        try { chain.doFilter(request, response); status = response.getStatus(); }
        finally {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String route = path.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "{id}");
            if (!route.matches("/health|/api/customers/\\{id\\}|/api/accounts/\\{id\\}(/transactions|/limit)?|/api/admin/statistics")) route = "/other";
            LOG.info("request method={} path={} status={} requestId={}", request.getMethod().matches("GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") ? request.getMethod() : "OTHER", route, status, trace);
        }
    }
    public static String id(HttpServletRequest request) { Object value = request.getAttribute("requestId"); return value instanceof String text ? text : "unavailable"; }
}
