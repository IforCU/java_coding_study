package kr.banklearning.jsplab;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 화면 연습용 fixture. 실제 고객·계좌·DB를 사용하지 않는다. */
public final class AccountLabServlet extends HttpServlet {
    private static final List<Map<String, Object>> ACCOUNTS = List.of(
            account("생활비 통장", "110-***-0001", "DEPOSIT", "ACTIVE", "1250000"),
            account("여행 적금", "220-***-0002", "SAVINGS", "ACTIVE", "350000"),
            account("비상금 <안전>", "110-***-0003", "DEPOSIT", "BLOCKED", "50000"),
            account("종료된 적금", "220-***-0004", "SAVINGS", "CLOSED", "0"));

    private static Map<String, Object> account(String name, String number, String type,
                                               String status, String balance) {
        return Map.of("name", name, "number", number, "type", type,
                "status", status, "balance", new BigDecimal(balance));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        request.setAttribute("accounts", "empty".equals(request.getParameter("demo"))
                ? List.of() : ACCOUNTS);
        request.getRequestDispatcher("/WEB-INF/views/accounts.jsp").forward(request, response);
    }
}
