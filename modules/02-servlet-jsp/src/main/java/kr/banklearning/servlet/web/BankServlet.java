package kr.banklearning.servlet.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import kr.banklearning.servlet.config.AppListener;
import kr.banklearning.servlet.dao.DataAccessException;
import kr.banklearning.servlet.service.BankService;
import kr.banklearning.servlet.service.InputException;
import kr.banklearning.servlet.service.MissingException;

@WebServlet(urlPatterns = "/", loadOnStartup = 1)
public final class BankServlet extends HttpServlet {
    private static final String CSRF = "csrfToken";
    private static final String SELECTED = "selectedCustomerId";
    private static final SecureRandom RANDOM = new SecureRandom();
    private BankService bank;

    public BankServlet() {}
    BankServlet(BankService bank) { this.bank = bank; }

    @Override
    public void init() throws ServletException {
        if (bank == null) bank = (BankService) getServletContext().getAttribute(AppListener.SERVICE);
        if (bank == null) throw new ServletException("학습 서비스를 초기화하지 못했습니다.");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            super.service(request, response);
        } catch (InputException exception) {
            error(response, 400, exception.getMessage());
        } catch (MissingException exception) {
            error(response, 404, exception.getMessage());
        } catch (DataAccessException exception) {
            error(response, 503, "데이터 저장소를 이용할 수 없습니다.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/health".equals(path)) {
            boolean ready = bank.ready();
            response.setStatus(ready ? 200 : 503);
            response.setContentType("application/json");
            response.getWriter().write(ready ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}");
            return;
        }
        if (path.isEmpty() || "/".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/customers");
            return;
        }
        HttpSession session = request.getSession(true);
        request.setAttribute(CSRF, token(session));
        request.setAttribute(SELECTED, session.getAttribute(SELECTED));
        switch (path) {
            case "/customers" -> {
                request.setAttribute("customers", bank.customers());
                forward("customers", request, response);
            }
            case "/accounts" -> {
                String customerId = request.getParameter("customerId");
                if (customerId == null) customerId = (String) session.getAttribute(SELECTED);
                request.setAttribute(SELECTED, customerId == null || customerId.isBlank() ? null : customerId);
                request.setAttribute("customers", bank.customers());
                request.setAttribute("accounts", bank.accounts(customerId));
                forward("accounts", request, response);
            }
            case "/history" -> {
                String accountId = request.getParameter("accountId");
                request.setAttribute("account", bank.account(accountId));
                request.setAttribute("transactions", bank.history(accountId));
                forward("history", request, response);
            }
            default -> error(response, 404, "페이지를 찾을 수 없습니다.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        String supplied = request.getParameter(CSRF);
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        if (expected == null || supplied == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            error(response, 403, "폼을 다시 열고 요청하세요.");
            return;
        }
        String target;
        switch (request.getServletPath()) {
            case "/customers" -> {
                var customer = bank.createCustomer(request.getParameter("name"));
                session.setAttribute(SELECTED, customer.id().toString());
                target = "/customers";
            }
            case "/accounts" -> {
                var account = bank.createAccount(request.getParameter("customerId"));
                session.setAttribute(SELECTED, account.customerId().toString());
                target = "/accounts";
            }
            case "/select-customer" -> {
                var customer = bank.customer(request.getParameter("customerId"));
                session.setAttribute(SELECTED, customer.id().toString());
                target = "/accounts";
            }
            default -> {
                error(response, 404, "페이지를 찾을 수 없습니다.");
                return;
            }
        }
        response.setStatus(303);
        response.setHeader("Location", request.getContextPath() + target);
    }

    private static String token(HttpSession session) {
        String token = (String) session.getAttribute(CSRF);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF, token);
        }
        return token;
    }

    private static void forward(String view, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    private static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain");
        response.getWriter().write(message);
    }
}
