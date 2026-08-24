package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
class WorkVerificationController {

    private final Companies companies;
    private final WorkVerificationService service;

    WorkVerificationController(Companies companies, WorkVerificationService service) {
        this.companies = companies;
        this.service = service;
    }

    @PostMapping("/companies/{id}/verify")
    String request(@PathVariable long id, @RequestParam String address,
                   HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        Company company = companies.findResolved(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return switch (service.request(member.get().id(), company, address.trim())) {
            case SENT -> "redirect:/companies/" + company.id() + "?verification=sent";
            case INVALID_ADDRESS -> "redirect:/companies/" + company.id() + "?verification=invalid";
            case RATE_LIMITED -> "redirect:/companies/" + company.id() + "?verification=limited";
            case NOT_ELIGIBLE -> throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        };
    }

    /** The scanner rule from login (§3.3): landing shows a button, only the button spends. */
    @GetMapping("/verify/{token}")
    String confirm(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "verify-confirm";
    }

    @PostMapping("/verify")
    String consume(@RequestParam String token) {
        return service.consume(token)
                .map(companyId -> "redirect:/companies/" + companyId + "?verification=done")
                .orElse("link-invalid");
    }
}
