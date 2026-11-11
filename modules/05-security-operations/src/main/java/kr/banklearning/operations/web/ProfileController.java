package kr.banklearning.operations.web;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import kr.banklearning.operations.service.BankProblem;
import kr.banklearning.operations.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public final class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/")
    public RedirectView home() {
        return redirect();
    }

    @GetMapping("/profile")
    public String profile(Principal principal, HttpServletRequest request, Model model) {
        model.addAttribute(
                "profile", service.ownProfile(principal.getName(), RequestTraceFilter.id(request)));
        return "profile";
    }

    @PostMapping("/profile/note")
    public RedirectView note(
            Principal principal, @RequestParam("note") String note, HttpServletRequest request) {
        service.updateNote(principal.getName(), note, RequestTraceFilter.id(request));
        return redirect();
    }

    @GetMapping("/admin/audit")
    public String audit(Principal principal, HttpServletRequest request, Model model) {
        model.addAttribute(
                "events", service.audit(principal.getName(), RequestTraceFilter.id(request)));
        return "audit";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        boolean ready = service.ready();
        return ResponseEntity.status(ready ? 200 : 503)
                .header("Content-Type", "application/json")
                .body(ready ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}");
    }

    @GetMapping("/csrf")
    @ResponseBody
    public ResponseEntity<String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String value = token.getToken();
        if (!value.matches("[A-Za-z0-9_=-]+"))
            throw new IllegalStateException("CSRF 토큰 형식을 확인하세요.");
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(
                        "{\"token\":\""
                                + value
                                + "\",\"parameterName\":\"_csrf\",\"headerName\":\"X-CSRF-TOKEN\"}");
    }

    @ExceptionHandler(BankProblem.class)
    @ResponseBody
    public ResponseEntity<String> business(BankProblem problem, HttpServletRequest request) {
        return error(problem.status(), problem.code(), request);
    }

    @ExceptionHandler({
        org.springframework.dao.DataAccessException.class,
        IllegalStateException.class
    })
    @ResponseBody
    public ResponseEntity<String> unavailable(HttpServletRequest request) {
        return error(503, "STORE_UNAVAILABLE", request);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<String> invalid(HttpServletRequest request) {
        return error(400, "INVALID_REQUEST", request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<String> unexpected(HttpServletRequest request) {
        return error(500, "INTERNAL_ERROR", request);
    }

    private static ResponseEntity<String> error(
            int status, String code, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(code + " requestId=" + RequestTraceFilter.id(request));
    }

    private static RedirectView redirect() {
        RedirectView view = new RedirectView("/profile", true);
        view.setStatusCode(HttpStatus.SEE_OTHER);
        view.setExposeModelAttributes(false);
        return view;
    }
}
