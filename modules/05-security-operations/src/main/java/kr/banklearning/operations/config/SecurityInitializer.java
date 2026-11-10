package kr.banklearning.operations.config;

import jakarta.servlet.ServletContext;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.CharacterEncodingFilter;

public final class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {
    @Override protected void beforeSpringSecurityFilterChain(ServletContext context) {
        insertFilters(context, new CharacterEncodingFilter("UTF-8", true));
    }
}
