package kr.banklearning.api.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import kr.banklearning.api.dto.ApiEnvelope;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.LimitView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.dto.BankViews.TransactionView;
import kr.banklearning.api.repository.BankReadRepository;
import kr.banklearning.api.service.BankReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankController {
    private final BankReadService service;
    private final BankReadRepository repository;

    public BankController(BankReadService service, BankReadRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/api/customers/{id}")
    public ApiEnvelope<CustomerView> customer(
            @PathVariable("id") UUID id, Authentication auth, HttpServletRequest request) {
        return ApiEnvelope.ok(
                service.customer(id, auth.getName(), admin(auth)), RequestTraceFilter.id(request));
    }

    @GetMapping("/api/accounts/{id}")
    public ApiEnvelope<AccountView> account(
            @PathVariable("id") UUID id, Authentication auth, HttpServletRequest request) {
        return ApiEnvelope.ok(
                service.account(id, auth.getName(), admin(auth)), RequestTraceFilter.id(request));
    }

    @GetMapping("/api/accounts/{id}/transactions")
    public ApiEnvelope<List<TransactionView>> transactions(
            @PathVariable("id") UUID id,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(200) int limit,
            Authentication auth,
            HttpServletRequest request) {
        return ApiEnvelope.ok(
                service.transactions(id, from, to, limit, auth.getName(), admin(auth)),
                RequestTraceFilter.id(request));
    }

    @GetMapping("/api/accounts/{id}/limit")
    public ApiEnvelope<LimitView> limit(
            @PathVariable("id") UUID id, Authentication auth, HttpServletRequest request) {
        return ApiEnvelope.ok(
                service.limit(id, auth.getName(), admin(auth)), RequestTraceFilter.id(request));
    }

    @GetMapping("/api/admin/statistics")
    public ApiEnvelope<StatisticsView> statistics(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            Authentication auth,
            HttpServletRequest request) {
        return ApiEnvelope.ok(
                service.statistics(from, to, admin(auth)), RequestTraceFilter.id(request));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean ready = repository.ready();
        return ResponseEntity.status(ready ? 200 : 503)
                .header("Content-Type", "application/json")
                .body(ready ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}");
    }

    private static boolean admin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
