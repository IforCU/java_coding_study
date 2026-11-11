package kr.banklearning.mvc.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import kr.banklearning.mvc.StubMapper;
import kr.banklearning.mvc.service.BankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BankControllerTest {
    private StubMapper mapper;
    private BankService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mapper = new StubMapper();
        service = new BankService(mapper);
        mvc =
                MockMvcBuilders.standaloneSetup(new BankController(service))
                        .setViewResolvers(
                                new org.springframework.web.servlet.view
                                        .InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                        .addInterceptors(new CsrfInterceptor())
                        .build();
    }

    @Test
    void getCreatesTokenAndSelectsPrivateJspModel() throws Exception {
        var result = mvc.perform(get("/customers")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertEquals("customers", result.getModelAndView().getViewName());
        assertEquals("/WEB-INF/views/customers.jsp", result.getResponse().getForwardedUrl());
        assertNotNull(result.getRequest().getSession(false).getAttribute("csrfToken"));
        assertNotNull(result.getModelAndView().getModel().get("customers"));
    }

    @Test
    void csrfRejectionPreventsRegistration() throws Exception {
        var result = mvc.perform(post("/customers").param("name", "학습고객01")).andReturn();
        assertEquals(403, result.getResponse().getStatus());
        assertTrue(mapper.customers.isEmpty());
    }

    @Test
    void acceptedPostUses303AndDoesNotExposeNameInRedirect() throws Exception {
        MockHttpSession session = session();
        var result =
                mvc.perform(
                                post("/customers")
                                        .session(session)
                                        .param("csrfToken", "test-token")
                                        .param("name", "학습고객01"))
                        .andReturn();
        assertEquals(303, result.getResponse().getStatus());
        assertEquals("/customers", result.getResponse().getRedirectedUrl());
        assertEquals(1, mapper.customers.size());
    }

    @Test
    void suspendedCustomerIs409WithoutAccountWrite() throws Exception {
        var customer = service.createCustomer("학습고객01");
        service.changeStatus(customer.id(), "SUSPENDED");
        var result =
                mvc.perform(
                                post("/accounts")
                                        .session(session())
                                        .param("csrfToken", "test-token")
                                        .param("customerId", customer.id()))
                        .andReturn();
        assertEquals(409, result.getResponse().getStatus());
        assertTrue(mapper.accounts.isEmpty());
    }

    @Test
    void invalidAndMissingAccountReturn400And404() throws Exception {
        assertEquals(
                400,
                mvc.perform(get("/history").param("accountId", "1-1-1-1-1"))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        assertEquals(
                404,
                mvc.perform(
                                get("/history")
                                        .param("accountId", java.util.UUID.randomUUID().toString()))
                        .andReturn()
                        .getResponse()
                        .getStatus());
    }

    @Test
    void invalidDateReturns400BeforeMapperHistoryRead() throws Exception {
        var account = service.createAccount(service.createCustomer("학습고객01").id());
        var result =
                mvc.perform(
                                get("/history")
                                        .param("accountId", account.id())
                                        .param("from", "2026-02-30"))
                        .andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertEquals(0, mapper.historyCalls);
    }

    @Test
    void persistenceFailureReturnsGeneric503() throws Exception {
        mapper.fail = true;
        var response = mvc.perform(get("/customers")).andReturn().getResponse();
        assertEquals(503, response.getStatus());
        assertFalse(response.getContentAsString().contains("secret"));
        assertFalse(response.getContentAsString().contains("jdbc:"));
    }

    @Test
    void healthReflectsDatabaseReadinessWithoutSession() throws Exception {
        var up = mvc.perform(get("/health")).andReturn();
        assertEquals(200, up.getResponse().getStatus());
        assertNull(up.getRequest().getSession(false));
        assertTrue(up.getResponse().getContentAsString().contains("UP"));
        mapper.ready = false;
        assertEquals(503, mvc.perform(get("/health")).andReturn().getResponse().getStatus());
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("csrfToken", "test-token");
        return session;
    }
}
