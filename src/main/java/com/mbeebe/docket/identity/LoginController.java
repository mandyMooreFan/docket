package com.mbeebe.docket.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class LoginController {

    private final MagicLinkService magicLinks;

    LoginController(MagicLinkService magicLinks) {
        this.magicLinks = magicLinks;
    }

    @GetMapping("/login")
    String form() {
        return "login";
    }

    @PostMapping("/login/link")
    String requestLink(@RequestParam String email, HttpServletRequest request, Model model) {
        MagicLinkService.Outcome outcome = magicLinks.requestLogin(email.trim(), request.getRemoteAddr());
        if (outcome == MagicLinkService.Outcome.SENT) {
            // The same answer whether or not the address has an account (§8.3).
            return "link-sent";
        }
        model.addAttribute("error", JoinController.emailError(outcome));
        return "login";
    }
}
