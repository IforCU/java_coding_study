package kr.banklearning.operations.config;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.filter.CharacterEncodingFilter;

class InitializerRegistrationTest {
    @Test
    void bothContainerDiscoveryOrdersRegisterOneEncodingFilterBeforeSecurity() throws Exception {
        for (boolean securityFirst : List.of(false, true)) {
            Registry registry = new Registry();
            ServletContext context = registry.context();
            if (securityFirst) {
                new SecurityInitializer().onStartup(context);
                new WebInitializer().onStartup(context);
            } else {
                new WebInitializer().onStartup(context);
                new SecurityInitializer().onStartup(context);
            }
            assertEquals(
                    Set.of("characterEncodingFilter", "springSecurityFilterChain"),
                    registry.filters.keySet());
            CharacterEncodingFilter encoding =
                    assertInstanceOf(
                            CharacterEncodingFilter.class,
                            registry.filters.get("characterEncodingFilter"));
            assertEquals("UTF-8", encoding.getEncoding());
            assertTrue(encoding.isForceRequestEncoding());
            assertEquals(
                    List.of("characterEncodingFilter", "springSecurityFilterChain"),
                    registry.urlMappings);
        }
    }

    /** Servlet API의 중복 이름 등록은 null을 반환한다. 컨텍스트 refresh/실제 Tomcat 실행은 하지 않는다. */
    private static final class Registry {
        final Map<String, Filter> filters = new LinkedHashMap<>();
        final List<String> urlMappings = new ArrayList<>();

        ServletContext context() {
            return proxy(
                    ServletContext.class,
                    (method, args) ->
                            switch (method) {
                                case "addFilter" -> {
                                    String name = (String) args[0];
                                    if (filters.putIfAbsent(name, (Filter) args[1]) != null)
                                        yield null;
                                    yield proxy(
                                            FilterRegistration.Dynamic.class,
                                            (operation, values) -> {
                                                if (operation.equals("addMappingForUrlPatterns"))
                                                    urlMappings.add(name);
                                                return null;
                                            });
                                }
                                case "addServlet" ->
                                        proxy(
                                                ServletRegistration.Dynamic.class,
                                                (operation, values) ->
                                                        operation.equals("addMapping")
                                                                ? Set.of()
                                                                : null);
                                case "getSessionCookieConfig" ->
                                        proxy(
                                                SessionCookieConfig.class,
                                                (operation, values) -> null);
                                default -> null;
                            });
        }
    }

    @FunctionalInterface
    private interface Operation {
        Object call(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Operation operation) {
        return (T)
                Proxy.newProxyInstance(
                        type.getClassLoader(),
                        new Class<?>[] {type},
                        (target, method, args) -> {
                            Object value = operation.call(method.getName(), args);
                            if (value != null) return value;
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                            if (method.getReturnType() == long.class) return 0L;
                            return null;
                        });
    }
}
