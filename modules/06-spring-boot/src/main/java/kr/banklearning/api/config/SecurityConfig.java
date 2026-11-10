package kr.banklearning.api.config;

import kr.banklearning.api.dto.ApiEnvelope;
import kr.banklearning.api.web.RequestTraceFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("learner").password(encoder.encode(password("DEMO_USER_PASSWORD", "learning-user-password"))).roles("USER").build(),
                User.withUsername("admin").password(encoder.encode(password("DEMO_ADMIN_PASSWORD", "learning-admin-password"))).roles("ADMIN").build());
    }
    @Bean public SecurityFilterChain security(HttpSecurity http, JsonMapper json) throws Exception {
        AuthenticationEntryPoint entry = (request, response, exception) -> {
            response.setStatus(401); response.setContentType("application/json;charset=UTF-8"); response.setHeader("WWW-Authenticate", "Basic realm=\"bank-learning-api\"");
            json.writeValue(response.getOutputStream(), ApiEnvelope.failure("UNAUTHORIZED", "인증이 필요합니다.", RequestTraceFilter.id(request)));
        };
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.requestMatchers("/health").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN").anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(entry))
                // 모든 업무 endpoint가 조회용 GET이며 브라우저 세션을 생성하지 않는다.
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entry).accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                    json.writeValue(response.getOutputStream(), ApiEnvelope.failure("FORBIDDEN", "요청 권한이 없습니다.", RequestTraceFilter.id(request)));
                }))
                .addFilterBefore(new RequestTraceFilter(), SecurityContextHolderFilter.class);
        return http.build();
    }
    private static String password(String name, String fallback) { String value = System.getenv(name); return value == null || value.isBlank() ? fallback : value; }
}
