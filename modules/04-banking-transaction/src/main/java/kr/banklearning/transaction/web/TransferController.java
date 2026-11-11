package kr.banklearning.transaction.web;

import java.math.BigDecimal;
import java.util.UUID;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.service.BankProblem;
import kr.banklearning.transaction.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public final class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @GetMapping("/")
    public RedirectView home() {
        return redirect("/accounts");
    }

    @GetMapping("/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", service.accounts());
        model.addAttribute("requestId", UUID.randomUUID());
        return "accounts";
    }

    @PostMapping("/transfers")
    public RedirectView transfer(
            @RequestParam("requestId") String requestId,
            @RequestParam("sourceId") String sourceId,
            @RequestParam("destinationId") String destinationId,
            @RequestParam("amount") String amount) {
        BigDecimal money;
        try {
            if (amount == null || amount.length() > 64) throw new NumberFormatException();
            money = new BigDecimal(amount);
        } catch (NumberFormatException exception) {
            throw new BankProblem(400, "INVALID_AMOUNT", "올바른 금액을 입력하세요.");
        }
        var receipt =
                service.transfer(
                        new TransferCommand(id(requestId), id(sourceId), id(destinationId), money));
        return redirect("/transfers/" + receipt.requestId());
    }

    @GetMapping("/transfers/{requestId}")
    public String receipt(@PathVariable("requestId") String requestId, Model model) {
        UUID request = id(requestId);
        model.addAttribute("receipt", service.get(request));
        model.addAttribute("ledger", service.ledger(request));
        return "receipt";
    }

    @PostMapping("/transfers/{requestId}/cancel")
    public RedirectView cancel(@PathVariable("requestId") String requestId) {
        var receipt = service.requestCancellation(id(requestId));
        return redirect("/transfers/" + receipt.requestId());
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        boolean ready = service.ready();
        return ResponseEntity.status(ready ? 200 : 503)
                .header("Content-Type", "application/json")
                .body(ready ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}");
    }

    @ExceptionHandler(BankProblem.class)
    @ResponseBody
    public ResponseEntity<String> business(BankProblem problem) {
        return ResponseEntity.status(problem.status())
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(problem.code() + ": " + problem.getMessage());
    }

    @ExceptionHandler({
        org.springframework.dao.DataAccessException.class,
        IllegalStateException.class
    })
    @ResponseBody
    public ResponseEntity<String> unavailable() {
        return ResponseEntity.status(503)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("데이터 저장소를 이용할 수 없습니다.");
    }

    private static UUID id(String value) {
        if (value == null
                || !value.matches(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new BankProblem(400, "INVALID_ID", "올바른 UUID 식별자를 입력하세요.");
        return UUID.fromString(value);
    }

    private static RedirectView redirect(String path) {
        RedirectView view = new RedirectView(path, true);
        view.setStatusCode(HttpStatus.SEE_OTHER);
        view.setExposeModelAttributes(false);
        return view;
    }
}
