package kr.banklearning.api.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.repository.BankReadRepository;
import kr.banklearning.api.service.BankReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:1/bank_learning", "spring.main.banner-mode=off"})
@AutoConfigureMockMvc
class BankApiContextTest {
    @Autowired MockMvc mvc;
    @Autowired HikariDataSource dataSource;
    @MockitoBean BankReadRepository repository;
    // 컨텍스트/JSON/보안 검증은 실제 DB 통합 검증과 분리한다.
    @MockitoBean PlatformTransactionManager transactionManager;
    private static final UUID ACCOUNT = UUID.fromString("44000000-0000-0000-0000-000000000001");
    @BeforeEach void setUp() {
        when(repository.account(ACCOUNT)).thenReturn(Optional.of(new AccountView(ACCOUNT, BankReadService.LEARNER_CUSTOMER, new BigDecimal("5000"), new BigDecimal("1000"), 2)));
    }
    @Test void bootAutoConfigurationCreatesReadOnlyPoolWithoutDatabaseConnection() {
        assertTrue(dataSource.isReadOnly()); assertEquals(0,dataSource.getMinimumIdle()); assertEquals("org.postgresql.Driver",dataSource.getDriverClassName());
    }
    @Test void missingAndInvalidCredentialsReturnStructured401() throws Exception {
        mvc.perform(get("/api/accounts/" + ACCOUNT).header("X-Request-ID","auth-1")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.requestId").value("auth-1"));
        mvc.perform(get("/api/accounts/" + ACCOUNT).with(httpBasic("learner","wrong-password"))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
    @Test void actualHttpBasicReturnsStandardSuccessEnvelopeWithoutSession() throws Exception {
        var result = mvc.perform(get("/api/accounts/" + ACCOUNT).with(httpBasic("learner",password("DEMO_USER_PASSWORD","learning-user-password"))).header("X-Request-ID","read-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.balance").value(5000))
                .andExpect(jsonPath("$.data.customerId").value(BankReadService.LEARNER_CUSTOMER.toString())).andExpect(jsonPath("$.requestId").value("read-1")).andReturn();
        assertNull(result.getRequest().getSession(false));
    }
    @Test void nineteenDigitBalanceIsSerializedAsExactJsonNumber() throws Exception {
        when(repository.account(ACCOUNT)).thenReturn(Optional.of(new AccountView(ACCOUNT, BankReadService.LEARNER_CUSTOMER,
                new BigDecimal("9999999999999999999"), new BigDecimal("1000"), 2)));
        String body = mvc.perform(get("/api/accounts/" + ACCOUNT).with(user("learner"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"balance\":9999999999999999999"));
        assertFalse(body.contains("\"balance\":\""));
    }
    @Test void learnerCannotReadForeignAccountOrAdminStatistics() throws Exception {
        UUID other = UUID.randomUUID(); when(repository.account(other)).thenReturn(Optional.of(new AccountView(other,UUID.randomUUID(),BigDecimal.TEN,BigDecimal.TEN,0)));
        mvc.perform(get("/api/accounts/" + other).with(user("learner"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mvc.perform(get("/api/admin/statistics").with(user("learner"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(repository,never()).statistics(any(),any());
    }
    @Test void adminStatisticsAndOwnCustomerAreSerialized() throws Exception {
        LocalDate date = LocalDate.of(2026,10,25); when(repository.statistics(date,date)).thenReturn(new StatisticsView(date,date,2,new BigDecimal("300")));
        mvc.perform(get("/api/admin/statistics").param("from","2026-10-25").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferCount").value(2)).andExpect(jsonPath("$.data.totalAmount").value(300));
        when(repository.customer(BankReadService.LEARNER_CUSTOMER)).thenReturn(Optional.of(new CustomerView(BankReadService.LEARNER_CUSTOMER,"학습고객01","ACTIVE")));
        mvc.perform(get("/api/customers/" + BankReadService.LEARNER_CUSTOMER).with(user("learner"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
    @Test void beanValidationRejectsOutOfBoundsLimitAndUuidBindingIs400() throws Exception {
        mvc.perform(get("/api/accounts/" + ACCOUNT + "/transactions").param("limit","201").with(user("learner")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/accounts/not-a-uuid").with(user("learner"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verify(repository,never()).transactions(any(),any(),any(),anyInt());
    }
    @Test void transactionQueryUsesDefaultLimitAndOptionalDates() throws Exception {
        when(repository.transactions(ACCOUNT,null,null,50)).thenReturn(List.of());
        mvc.perform(get("/api/accounts/" + ACCOUNT + "/transactions").with(user("learner"))).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        verify(repository).transactions(ACCOUNT,null,null,50);
    }
    @Test void malformedDateAndMissingAccountUseCommonErrors() throws Exception {
        mvc.perform(get("/api/accounts/" + ACCOUNT + "/transactions").param("from","2026-02-30").with(user("learner"))).andExpect(status().isBadRequest());
        UUID missing = UUID.randomUUID(); when(repository.account(missing)).thenReturn(Optional.empty());
        mvc.perform(get("/api/accounts/" + missing).with(user("learner"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
    @Test void sqlFailureIs503AndDoesNotLeakInternalDetails() throws Exception {
        when(repository.account(ACCOUNT)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("jdbc:secret password=secret"));
        var result = mvc.perform(get("/api/accounts/" + ACCOUNT).with(user("learner")).header("X-Request-ID","failure-1"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.code").value("STORE_UNAVAILABLE")).andExpect(jsonPath("$.requestId").value("failure-1")).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("secret")); assertFalse(result.getResponse().getContentAsString().contains("jdbc:"));
    }
    @Test void publicHealthAndUnsafeTraceHeaderUseSafeResponse() throws Exception {
        when(repository.ready()).thenReturn(true);
        var result = mvc.perform(get("/health").header("X-Request-ID","email@example.test")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")).andReturn();
        assertTrue(result.getResponse().getHeader("X-Request-ID").matches("[0-9a-f-]{36}"));
        when(repository.ready()).thenReturn(false); mvc.perform(get("/health")).andExpect(status().isServiceUnavailable());
    }
    private static String password(String name,String fallback) { String value = System.getenv(name); return value == null || value.isBlank() ? fallback : value; }
}
