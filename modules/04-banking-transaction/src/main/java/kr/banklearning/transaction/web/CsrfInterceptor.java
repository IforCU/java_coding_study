package kr.banklearning.transaction.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.web.servlet.HandlerInterceptor;

public final class CsrfInterceptor implements HandlerInterceptor {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if ("/health".equals(request.getRequestURI().substring(request.getContextPath().length())))
            return true;
        if ("POST".equals(request.getMethod())) {
            HttpSession session = request.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute("csrfToken");
            String actual = request.getParameter("csrfToken");
            if (expected == null
                    || actual == null
                    || !MessageDigest.isEqual(
                            expected.getBytes(StandardCharsets.UTF_8),
                            actual.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(403);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("폼을 다시 열고 요청하세요.");
                return false;
            }
        } else {
            HttpSession session = request.getSession(true);
            String token = (String) session.getAttribute("csrfToken");
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute("csrfToken", token);
            }
            request.setAttribute("csrfToken", token);
        }
        return true;
    }
}
