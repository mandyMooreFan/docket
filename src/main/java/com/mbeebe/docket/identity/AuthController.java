package com.mbeebe.docket.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class AuthController {

    private final MagicLinkService magicLinks;
    private final SessionService sessions;

    AuthController(MagicLinkService magicLinks, SessionService sessions) {
        this.sessions = sessions;
        this.magicLinks = magicLinks;
    }

    /**
     * The link lands on a confirmation, and only the button consumes the token —
     * an email scanner that follows links must not use one up.
     */
    @GetMapping("/auth/{token}")
    String confirm(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "auth-confirm";
    }

    @PostMapping("/auth")
    String consume(@RequestParam String token, HttpServletRequest request,
                   HttpServletResponse response) {
        return magicLinks.consume(token)
                .map(member -> {
                    sessions.start(member, request, response);
                    return "redirect:/";
                })
                .orElse("link-invalid");
    }
}
