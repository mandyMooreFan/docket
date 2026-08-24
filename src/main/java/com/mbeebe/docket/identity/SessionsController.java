package com.mbeebe.docket.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** The visible session list, one-tap sign-out-everywhere (§3.3). */
@Controller
class SessionsController {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm").withZone(ZoneId.systemDefault());

    private final SessionService sessions;

    SessionsController(SessionService sessions) {
        this.sessions = sessions;
    }

    record SessionRow(String client, String started, String lastUsed, boolean current) {
    }

    @GetMapping("/settings/sessions")
    String list(HttpServletRequest request, Model model) {
        return CurrentMember.get(request)
                .map(member -> {
                    model.addAttribute("sessions", sessions.sessionsFor(member, request).stream()
                            .map(s -> new SessionRow(s.client(), WHEN.format(s.createdAt()),
                                    WHEN.format(s.lastUsedAt()), s.current()))
                            .toList());
                    return "sessions";
                })
                .orElse("redirect:/login");
    }

    @PostMapping("/settings/sessions/sign-out")
    String signOut(HttpServletRequest request, HttpServletResponse response) {
        sessions.signOut(request, response);
        return "redirect:/";
    }

    @PostMapping("/settings/sessions/sign-out-everywhere")
    String signOutEverywhere(HttpServletRequest request, HttpServletResponse response) {
        CurrentMember.get(request)
                .ifPresent(member -> sessions.signOutEverywhere(member, request, response));
        return "redirect:/";
    }
}
