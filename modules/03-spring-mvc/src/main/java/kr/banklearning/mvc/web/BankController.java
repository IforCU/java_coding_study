package kr.banklearning.mvc.web;

import java.util.Map;
import kr.banklearning.mvc.service.BankProblem;
import kr.banklearning.mvc.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public final class BankController {
    private final BankService bank;

    public BankController(BankService bank) {
        this.bank = bank;
    }

    @GetMapping("/")
    public RedirectView home() {
        return redirect("/customers");
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", bank.customers());
        return "customers";
    }

    @PostMapping("/customers")
    public RedirectView createCustomer(@RequestParam("name") String name) {
        bank.createCustomer(name);
        return redirect("/customers");
    }

    @PostMapping("/customers/status")
    public RedirectView changeStatus(
            @RequestParam("customerId") String customerId, @RequestParam("status") String status) {
        bank.changeStatus(customerId, status);
        return redirect("/customers");
    }

    @GetMapping("/accounts")
    public String accounts(
            @RequestParam(name = "customerId", required = false) String customerId, Model model) {
        model.addAttribute("customers", bank.customers());
        model.addAttribute("accounts", bank.accounts(customerId));
        return "accounts";
    }

    @PostMapping("/accounts")
    public RedirectView createAccount(@RequestParam("customerId") String customerId) {
        bank.createAccount(customerId);
        return redirect("/accounts");
    }

    @GetMapping("/history")
    public String history(
            @RequestParam("accountId") String accountId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            Model model) {
        model.addAttribute("account", bank.account(accountId));
        model.addAttribute("transactions", bank.history(accountId, from, to));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "history";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        boolean ready = bank.ready();
        return ResponseEntity.status(ready ? 200 : 503)
                .header("Content-Type", "application/json")
                .body(ready ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}");
    }

    @ExceptionHandler(BankProblem.class)
    @ResponseBody
    public ResponseEntity<String> business(BankProblem problem) {
        return ResponseEntity.status(problem.status())
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(problem.getMessage());
    }

    @ExceptionHandler({
        org.springframework.dao.DataAccessException.class,
        org.apache.ibatis.exceptions.PersistenceException.class,
        IllegalStateException.class
    })
    @ResponseBody
    public ResponseEntity<String> unavailable() {
        return ResponseEntity.status(503)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("데이터 저장소를 이용할 수 없습니다.");
    }

    private static RedirectView redirect(String path) {
        RedirectView view = new RedirectView(path, true);
        view.setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
        view.setExposeModelAttributes(false);
        return view;
    }
}
