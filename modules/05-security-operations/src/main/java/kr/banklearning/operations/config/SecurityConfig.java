package kr.banklearning.operations.config;

import kr.banklearning.operations.service.AuditService;
import kr.banklearning.operations.web.RequestTraceFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("learner").password(encoder.encode(password("DEMO_USER_PASSWORD", "learning-user-password"))).roles("USER").build(),
                User.withUsername("admin").password(encoder.encode(password("DEMO_ADMIN_PASSWORD", "learning-admin-password"))).roles("ADMIN").build());
    }
    @Bean public AccessDeniedHandler accessDeniedHandler(AuditService audit) {
        return (request, response, exception) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            audit.record(authentication == null ? null : authentication.getName(), "ACCESS", "DENIED", RequestTraceFilter.id(request));
            response.setStatus(403); response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("FORBIDDEN requestId=" + RequestTraceFilter.id(request));
        };
    }
    @Bean public SecurityFilterChain security(HttpSecurity http, AccessDeniedHandler denied) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/health").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/profile", true).permitAll())
                .httpBasic(Customizer.withDefaults()).csrf(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(denied))
                .addFilterBefore(new RequestTraceFilter(), SecurityContextHolderFilter.class);
        return http.build();
    }
    private static String password(String name, String fallback) {
        String value = System.getenv(name); return value == null || value.isBlank() ? fallback : value;
    }
}
