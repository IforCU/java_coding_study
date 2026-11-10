package kr.banklearning.operations.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.Clock;
import kr.banklearning.operations.MemoryProfileRepository;
import kr.banklearning.operations.config.SecurityConfig;
import kr.banklearning.operations.config.WebConfig;
import kr.banklearning.operations.domain.Profile;
import kr.banklearning.operations.domain.ProfileView;
import kr.banklearning.operations.service.AuditService;
import kr.banklearning.operations.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class SecurityFlowTest {
    private AnnotationConfigWebApplicationContext context;
    private MemoryProfileRepository repository;
    private MockMvc mvc;
    @BeforeEach void setUp() {
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(TestConfig.class, SecurityConfig.class, WebConfig.class); context.refresh();
        repository = context.getBean(MemoryProfileRepository.class);
        repository.profiles.put("learner", new Profile("learner", "학습사용자", "learner@example.test", "123456789012", "first"));
        repository.profiles.put("admin", new Profile("admin", "학습관리자", "admin@example.test", "111122223333", "admin"));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void close() { if (context != null) context.close(); }
    @Test void anonymousBrowserRedirectsToLoginAndApiClientReceives401() throws Exception {
        var browser = mvc.perform(get("/profile").header("Accept", "text/html")).andReturn().getResponse();
        assertEquals(302, browser.getStatus()); assertTrue(browser.getRedirectedUrl().endsWith("/login"));
        assertEquals(401, mvc.perform(get("/profile").header("Accept", "application/json")).andReturn().getResponse().getStatus());
    }
    @Test void credentialsAreStoredWithBcryptAndHttpBasicAuthenticates() throws Exception {
        var user = context.getBean(UserDetailsService.class).loadUserByUsername("learner");
        assertTrue(user.getPassword().startsWith("$2"));
        assertTrue(context.getBean(PasswordEncoder.class).matches(password("DEMO_USER_PASSWORD", "learning-user-password"), user.getPassword()));
        assertEquals(200, mvc.perform(get("/profile").with(httpBasic("learner", password("DEMO_USER_PASSWORD", "learning-user-password")))).andReturn().getResponse().getStatus());
    }
    @Test void userCannotReadAdminAuditAndDenialIsAudited() throws Exception {
        var result = mvc.perform(get("/admin/audit").with(user("learner").roles("USER")).header("X-Request-ID", "deny-1")).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(repository.events.stream().anyMatch(event -> event.outcome().equals("DENIED") && event.requestId().equals("deny-1")));
        assertTrue(result.getResponse().getContentAsString().contains("deny-1"));
    }
    @Test void adminCanReadAuditedPrivateJsp() throws Exception {
        var result = mvc.perform(get("/admin/audit").with(user("admin").roles("ADMIN"))).andReturn();
        assertEquals(200, result.getResponse().getStatus()); assertEquals("/WEB-INF/views/audit.jsp", result.getResponse().getForwardedUrl());
        assertTrue(repository.events.stream().anyMatch(event -> event.action().equals("AUDIT_READ")));
    }
    @Test void missingCsrfIs403AndLeavesNoteUnchanged() throws Exception {
        assertEquals(403, mvc.perform(post("/profile/note").with(user("learner")).param("note", "changed")).andReturn().getResponse().getStatus());
        assertEquals("first", repository.profiles.get("learner").note());
    }
    @Test void csrfProtectedUpdateIgnoresForgedUsernameAndUsesPrg() throws Exception {
        var response = mvc.perform(post("/profile/note").with(user("learner")).with(csrf()).param("note", "changed").param("username", "admin")).andReturn().getResponse();
        assertEquals(303, response.getStatus()); assertEquals("/profile", response.getRedirectedUrl());
        assertEquals("changed", repository.profiles.get("learner").note()); assertEquals("admin", repository.profiles.get("admin").note());
    }
    @Test void profileIgnoresForgedOwnerAndMasksFields() throws Exception {
        var result = mvc.perform(get("/profile").with(user("learner")).param("username", "admin")).andReturn();
        ProfileView profile = (ProfileView) result.getModelAndView().getModel().get("profile");
        assertEquals("learner", profile.username()); assertEquals("l***@example.test", profile.email()); assertEquals("****9012", profile.accountHint());
    }
    @Test void unsafeRequestIdIsReplacedAndSafeIdIsPreserved() throws Exception {
        var valid = mvc.perform(get("/health").header("X-Request-ID", "trace-ABC-01")).andReturn();
        assertEquals("trace-ABC-01", valid.getResponse().getHeader("X-Request-ID"));
        var invalid = mvc.perform(get("/health").header("X-Request-ID", "secret@example.test")).andReturn();
        assertTrue(invalid.getResponse().getHeader("X-Request-ID").matches("[0-9a-f-]{36}"));
        assertFalse(invalid.getResponse().getHeader("X-Request-ID").contains("secret"));
    }
    @Test void errorsContainOnlyCodeAndRequestIdWithoutSqlDetails() throws Exception {
        repository.fail = true;
        var response = mvc.perform(get("/profile").with(user("learner")).header("X-Request-ID", "store-1")).andReturn().getResponse();
        assertEquals(503, response.getStatus()); assertEquals("STORE_UNAVAILABLE requestId=store-1", response.getContentAsString());
        assertFalse(response.getContentAsString().contains("secret"));
    }
    @Test void invalidNoteIs400AndCsrfEndpointRequiresAuthentication() throws Exception {
        var response = mvc.perform(post("/profile/note").with(user("learner")).with(csrf()).param("note", " ")).andReturn().getResponse();
        assertEquals(400, response.getStatus());
        assertEquals(401, mvc.perform(get("/csrf").header("Accept", "application/json")).andReturn().getResponse().getStatus());
        var token = mvc.perform(get("/csrf").with(user("learner"))).andReturn().getResponse();
        assertEquals(200, token.getStatus()); assertTrue(token.getContentAsString().contains("\"token\""));
    }
    @Test void formLoginRotatesSessionAndExpiredSessionRequiresLogin() throws Exception {
        var page = mvc.perform(get("/login")).andReturn();
        MockHttpSession previous = (MockHttpSession) page.getRequest().getSession();
        String previousId = previous.getId();
        var login = mvc.perform(post("/login").session(previous).with(request -> {
            request.setRequestedSessionId(previousId);
            request.setRequestedSessionIdValid(true);
            request.setRequestedSessionIdFromCookie(true);
            return request;
        }).with(csrf()).param("username", "learner").param("password", password("DEMO_USER_PASSWORD", "learning-user-password"))).andReturn();
        assertEquals(302, login.getResponse().getStatus());
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false); assertNotNull(session);
        assertNotEquals(previousId, session.getId());
        assertEquals(200, mvc.perform(get("/profile").session(session)).andReturn().getResponse().getStatus());
        session.invalidate();
        assertEquals(302, mvc.perform(get("/profile").header("Accept", "text/html")).andReturn().getResponse().getStatus());
    }
    @Test void healthIsPublicAndReflectsReadiness() throws Exception {
        assertEquals(200, mvc.perform(get("/health")).andReturn().getResponse().getStatus()); repository.ready = false;
        assertEquals(503, mvc.perform(get("/health")).andReturn().getResponse().getStatus());
    }
    private static String password(String name, String fallback) { String value = System.getenv(name); return value == null || value.isBlank() ? fallback : value; }
    @Configuration static class TestConfig {
        @Bean MemoryProfileRepository repository() { return new MemoryProfileRepository(); }
        @Bean AuditService auditService(MemoryProfileRepository repository) { return new AuditService(repository, Clock.systemUTC()); }
        @Bean ProfileService profileService(MemoryProfileRepository repository, AuditService audit) { return new ProfileService(repository, audit); }
    }
}
