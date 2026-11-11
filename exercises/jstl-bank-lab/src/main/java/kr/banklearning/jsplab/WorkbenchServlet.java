package kr.banklearning.jsplab;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Phase 3~6 화면용 고정 fixture. 금융 거래·인증·외부 API 호출을 수행하지 않는다. */
public final class WorkbenchServlet extends HttpServlet {
    private static final List<Map<String, Object>> HISTORY = List.of(
            history("H01", "DEPOSIT", "급여 <정산>", "1500000", "2026-11-01T00:00:00Z"),
            history("H02", "WITHDRAWAL", "관리비", "120000", "2026-11-02T00:00:00Z"),
            history("H03", "TRANSFER", "여행 적립", "50000", "2026-11-03T00:00:00Z"));
    private static final List<Map<String, Object>> TRANSFERS = List.of(
            Map.of("id", "T01", "status", "COMPLETED", "amount", new BigDecimal("100000")),
            Map.of("id", "T02", "status", "PENDING", "amount", new BigDecimal("50000")),
            Map.of("id", "T03", "status", "CANCEL_REQUESTED", "amount", new BigDecimal("30000")));
    // 원문 개인정보는 JSP에 전달하지 않는다. UI 숨김으로 권한을 구현하지 않는다.
    private static final List<Map<String, Object>> AUDIT = List.of(
            Map.of("id", "A01", "account", "110-***-0001", "action", "조회 <확인>", "requestId", "req-lab-01"),
            Map.of("id", "A02", "account", "220-***-0002", "action", "접근 거절", "requestId", "req-lab-02"));

    private static Map<String, Object> history(String id, String type, String memo, String amount, String time) {
        return Map.of("id", id, "type", type, "memo", memo, "amount", new BigDecimal(amount),
                "occurredAt", Date.from(Instant.parse(time)));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        boolean empty = "empty".equals(request.getParameter("demo"));
        boolean allowed = !"denied".equals(request.getParameter("demo"));
        boolean available = !"unavailable".equals(request.getParameter("demo"));
        request.setAttribute("history", empty ? List.of() : HISTORY);
        request.setAttribute("transfers", empty ? List.of() : TRANSFERS);
        request.setAttribute("canViewAudit", allowed);
        request.setAttribute("audit", allowed && !empty ? AUDIT : List.of());
        request.setAttribute("api", Map.of("success", available, "requestId", "api-lab-01",
                "message", available ? "조회 완료" : "잠시 후 다시 조회해 주세요.",
                "data", Map.of("accounts", available && !empty ? List.of(
                        Map.of("name", "생활비 <통장>", "balance", new BigDecimal("1250000")),
                        Map.of("name", "여행 적금", "balance", new BigDecimal("350000"))) : List.of())));
        request.getRequestDispatcher("/WEB-INF/views/workbench.jsp").forward(request, response);
    }
}
