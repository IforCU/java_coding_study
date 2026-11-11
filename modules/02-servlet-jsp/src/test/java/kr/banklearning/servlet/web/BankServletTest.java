package kr.banklearning.servlet.web;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.servlet.dao.BankDao;
import kr.banklearning.servlet.dao.DataAccessException;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import kr.banklearning.servlet.dto.BankViews.TransactionView;
import kr.banklearning.servlet.service.BankService;
import org.junit.jupiter.api.Test;

class BankServletTest {
    @Test
    void getForwardsToPrivateJspAndProvidesStableSessionToken() throws Exception {
        Exchange exchange = new Exchange();
        BankServlet servlet = new BankServlet(new BankService(exchange.dao));
        servlet.service(exchange.request(), exchange.response());
        assertEquals("/WEB-INF/views/customers.jsp", exchange.forward);
        String token = (String) exchange.sessionValues.get("csrfToken");
        assertNotNull(token);
        assertTrue(token.length() >= 32);
        assertEquals(token, exchange.attributes.get("csrfToken"));
        servlet.service(exchange.request(), exchange.response());
        assertEquals(token, exchange.sessionValues.get("csrfToken"));
    }

    @Test
    void csrfFailureDoesNotWriteAndDoesNotCreateSession() throws Exception {
        Exchange exchange = new Exchange();
        exchange.method = "POST";
        exchange.parameters.put("name", "학습고객");
        new BankServlet(new BankService(exchange.dao))
                .service(exchange.request(), exchange.response());
        assertEquals(403, exchange.status);
        assertTrue(exchange.dao.customers.isEmpty());
        assertFalse(exchange.sessionCreated);
    }

    @Test
    void successfulPostRegistersThenRedirectsAndStoresSelectedCustomer() throws Exception {
        Exchange exchange = new Exchange();
        exchange.method = "POST";
        exchange.sessionCreated = true;
        exchange.sessionValues.put("csrfToken", "known-test-token");
        exchange.parameters.putAll(Map.of("csrfToken", "known-test-token", "name", "학습고객"));
        new BankServlet(new BankService(exchange.dao))
                .service(exchange.request(), exchange.response());
        assertEquals(303, exchange.status);
        assertEquals("/bank/customers", exchange.headers.get("Location"));
        assertEquals(1, exchange.dao.customers.size());
        assertEquals(
                exchange.dao.customers.get(0).id().toString(),
                exchange.sessionValues.get("selectedCustomerId"));
        assertNull(exchange.forward);
    }

    @Test
    void malformedAccountIdReturns400AndMissingAccountReturns404() throws Exception {
        Exchange exchange = new Exchange();
        exchange.path = "/history";
        exchange.parameters.put("accountId", "not-uuid");
        BankServlet servlet = new BankServlet(new BankService(exchange.dao));
        servlet.service(exchange.request(), exchange.response());
        assertEquals(400, exchange.status);
        exchange.parameters.put("accountId", UUID.randomUUID().toString());
        servlet.service(exchange.request(), exchange.response());
        assertEquals(404, exchange.status);
    }

    @Test
    void unavailableStoreReturns503WithoutInternalDetails() throws Exception {
        Exchange exchange = new Exchange();
        exchange.dao.fail = true;
        new BankServlet(new BankService(exchange.dao))
                .service(exchange.request(), exchange.response());
        assertEquals(503, exchange.status);
        assertFalse(exchange.body.toString().contains("jdbc:"));
        assertFalse(exchange.body.toString().contains("Exception"));
    }

    @Test
    void healthChecksReadinessWithoutCreatingSession() throws Exception {
        Exchange exchange = new Exchange();
        exchange.path = "/health";
        BankServlet servlet = new BankServlet(new BankService(exchange.dao));
        servlet.service(exchange.request(), exchange.response());
        assertEquals(200, exchange.status);
        assertTrue(exchange.body.toString().contains("UP"));
        assertFalse(exchange.sessionCreated);
        exchange.dao.ready = false;
        servlet.service(exchange.request(), exchange.response());
        assertEquals(503, exchange.status);
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        Exchange exchange = new Exchange();
        exchange.path = "/unknown";
        new BankServlet(new BankService(exchange.dao))
                .service(exchange.request(), exchange.response());
        assertEquals(404, exchange.status);
    }

    private static final class Dao implements BankDao {
        final List<CustomerView> customers = new ArrayList<>();
        boolean fail;
        boolean ready = true;

        public List<CustomerView> customers() {
            if (fail) throw new DataAccessException();
            return customers;
        }

        public Optional<CustomerView> customer(UUID id) {
            return customers.stream().filter(customer -> customer.id().equals(id)).findFirst();
        }

        public void insertCustomer(CustomerView customer) {
            customers.add(customer);
        }

        public List<AccountView> accounts(UUID id) {
            return List.of();
        }

        public Optional<AccountView> account(UUID id) {
            return Optional.empty();
        }

        public void insertAccount(AccountView account) {}

        public List<TransactionView> history(UUID id) {
            return List.of();
        }

        public boolean ready() {
            return ready;
        }
    }

    private static final class Exchange {
        final Dao dao = new Dao();
        final Map<String, String> parameters = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final Map<String, Object> sessionValues = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        final StringWriter body = new StringWriter();
        String method = "GET", path = "/customers", forward;
        int status = 200;
        boolean sessionCreated;

        HttpServletRequest request() {
            return proxy(
                    HttpServletRequest.class,
                    (name, args) ->
                            switch (name) {
                                case "getMethod" -> method;
                                case "getServletPath" -> path;
                                case "getContextPath" -> "/bank";
                                case "getParameter" -> parameters.get((String) args[0]);
                                case "getAttribute" -> attributes.get((String) args[0]);
                                case "setAttribute" -> {
                                    attributes.put((String) args[0], args[1]);
                                    yield null;
                                }
                                case "getDateHeader" -> -1L;
                                case "getSession" -> {
                                    boolean create =
                                            args == null || args.length == 0 || (boolean) args[0];
                                    if (!create && !sessionCreated) yield null;
                                    sessionCreated = true;
                                    yield session();
                                }
                                case "getRequestDispatcher" -> {
                                    String destination = (String) args[0];
                                    yield proxy(
                                            RequestDispatcher.class,
                                            (operation, values) -> {
                                                if (operation.equals("forward"))
                                                    forward = destination;
                                                return null;
                                            });
                                }
                                default -> null;
                            });
        }

        HttpSession session() {
            return proxy(
                    HttpSession.class,
                    (name, args) ->
                            switch (name) {
                                case "getAttribute" -> sessionValues.get((String) args[0]);
                                case "setAttribute" -> {
                                    sessionValues.put((String) args[0], args[1]);
                                    yield null;
                                }
                                default -> null;
                            });
        }

        HttpServletResponse response() {
            return proxy(
                    HttpServletResponse.class,
                    (name, args) ->
                            switch (name) {
                                case "setStatus" -> {
                                    status = (int) args[0];
                                    yield null;
                                }
                                case "setHeader" -> {
                                    headers.put((String) args[0], (String) args[1]);
                                    yield null;
                                }
                                case "getWriter" -> new PrintWriter(body);
                                case "sendRedirect" -> {
                                    status = 302;
                                    headers.put("Location", (String) args[0]);
                                    yield null;
                                }
                                default -> null;
                            });
        }
    }

    @FunctionalInterface
    private interface Operation {
        Object apply(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Operation operation) {
        return (T)
                Proxy.newProxyInstance(
                        type.getClassLoader(),
                        new Class<?>[] {type},
                        (target, method, args) -> {
                            Object result = operation.apply(method.getName(), args);
                            if (result != null) return result;
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                            if (method.getReturnType() == long.class) return 0L;
                            return null;
                        });
    }
}
