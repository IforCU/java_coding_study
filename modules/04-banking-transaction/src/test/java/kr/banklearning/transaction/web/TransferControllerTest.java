package kr.banklearning.transaction.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import kr.banklearning.transaction.MemoryTransferRepository;
import kr.banklearning.transaction.domain.Account;
import kr.banklearning.transaction.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class TransferControllerTest {
    private MemoryTransferRepository repository;
    private MockMvc mvc;
    private UUID source, destination;

    @BeforeEach
    void setUp() {
        repository = new MemoryTransferRepository();
        source = UUID.randomUUID();
        destination = UUID.randomUUID();
        repository.accounts.put(
                source,
                new Account(
                        source,
                        UUID.randomUUID(),
                        new BigDecimal("5000"),
                        new BigDecimal("1000"),
                        0));
        repository.accounts.put(
                destination,
                new Account(
                        destination,
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        new BigDecimal("1000"),
                        0));
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new TransferController(
                                        new TransferService(
                                                repository, Clock.systemUTC(), () -> {})))
                        .setViewResolvers(
                                new InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                        .addInterceptors(new CsrfInterceptor())
                        .build();
    }

    @Test
    void formProvidesSessionTokenAndFreshRequestId() throws Exception {
        var result = mvc.perform(get("/accounts")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertEquals("/WEB-INF/views/accounts.jsp", result.getResponse().getForwardedUrl());
        assertNotNull(result.getRequest().getSession(false).getAttribute("csrfToken"));
        assertInstanceOf(UUID.class, result.getModelAndView().getModel().get("requestId"));
    }

    @Test
    void csrfFailureDoesNotReserveRequest() throws Exception {
        assertEquals(403, mvc.perform(post("/transfers")).andReturn().getResponse().getStatus());
        assertEquals(0, repository.reserveCalls);
    }

    @Test
    void successfulTransferRedirectsToReceiptAndReceiptShowsTwoLedgerEntries() throws Exception {
        UUID id = UUID.randomUUID();
        var result =
                mvc.perform(
                                post("/transfers")
                                        .session(session())
                                        .param("csrfToken", "test-token")
                                        .param("requestId", id.toString())
                                        .param("sourceId", source.toString())
                                        .param("destinationId", destination.toString())
                                        .param("amount", "300"))
                        .andReturn();
        assertEquals(303, result.getResponse().getStatus());
        assertEquals("/transfers/" + id, result.getResponse().getRedirectedUrl());
        var receipt = mvc.perform(get("/transfers/" + id)).andReturn();
        assertEquals(200, receipt.getResponse().getStatus());
        assertEquals("/WEB-INF/views/receipt.jsp", receipt.getResponse().getForwardedUrl());
        assertEquals(
                2, ((java.util.List<?>) receipt.getModelAndView().getModel().get("ledger")).size());
    }

    @Test
    void malformedAmountAndUuidAre400BeforeReservation() throws Exception {
        var request =
                post("/transfers")
                        .session(session())
                        .param("csrfToken", "test-token")
                        .param("requestId", UUID.randomUUID().toString())
                        .param("sourceId", source.toString())
                        .param("destinationId", destination.toString())
                        .param("amount", "not-money");
        assertEquals(400, mvc.perform(request).andReturn().getResponse().getStatus());
        assertEquals(
                400,
                mvc.perform(get("/transfers/1-1-1-1-1")).andReturn().getResponse().getStatus());
        assertEquals(0, repository.reserveCalls);
    }

    @Test
    void unknownReceiptIs404AndHealthAvoidsSession() throws Exception {
        assertEquals(
                404,
                mvc.perform(get("/transfers/" + UUID.randomUUID()))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        var health = mvc.perform(get("/health")).andReturn();
        assertEquals(200, health.getResponse().getStatus());
        assertNull(health.getRequest().getSession(false));
        repository.ready = false;
        assertEquals(503, mvc.perform(get("/health")).andReturn().getResponse().getStatus());
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("csrfToken", "test-token");
        return session;
    }
}
