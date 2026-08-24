package com.mbeebe.docket.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Arrays;

/**
 * §3.1: the age ask is the first screen, before any email is collected. A neutral
 * question, every answer available; a refusal stores nothing server-side, and an
 * adult's month/year evaporates right here — only the kind travels on.
 */
@Controller
class JoinController {

    /** Device-local (~24h) resubmission block after a refusal — a cookie, nothing server-side. */
    static final String AGE_HOLD_COOKIE = "docket_age_hold";

    private final MagicLinkService magicLinks;
    private final Clock clock;

    JoinController(MagicLinkService magicLinks, Clock clock) {
        this.magicLinks = magicLinks;
        this.clock = clock;
    }

    @GetMapping("/join")
    String ageAsk(HttpServletRequest request, Model model) {
        if (held(request)) {
            return "join-refused";
        }
        model.addAttribute("currentYear", YearMonth.now(clock).getYear());
        return "join-age";
    }

    @PostMapping("/join")
    String declareAge(@RequestParam int month, @RequestParam int year,
                      HttpServletRequest request, HttpServletResponse response, Model model) {
        if (held(request)) {
            return "join-refused";
        }
        YearMonth now = YearMonth.now(clock);
        if (month < 1 || month > 12 || year < 1900 || year > now.getYear()) {
            model.addAttribute("currentYear", now.getYear());
            model.addAttribute("error", "That isn't a month and year.");
            return "join-age";
        }
        YearMonth birth = YearMonth.of(year, month);
        if (!AgeRules.reached(16, birth, now)) {
            response.addCookie(ageHold(request));
            return "join-refused";
        }
        if (AgeRules.reached(18, birth, now)) {
            // The adult's month and year go no further than this method (§9.3).
            model.addAttribute("ageKind", Member.AgeKind.ADULT);
        } else {
            model.addAttribute("ageKind", Member.AgeKind.MINOR);
            model.addAttribute("birthMonth", month);
            model.addAttribute("birthYear", year);
        }
        return "join-email";
    }

    @PostMapping("/join/link")
    String requestLink(@RequestParam String email, @RequestParam Member.AgeKind ageKind,
                       @RequestParam(required = false) Integer birthMonth,
                       @RequestParam(required = false) Integer birthYear,
                       HttpServletRequest request, HttpServletResponse response, Model model) {
        YearMonth birth = null;
        if (ageKind == Member.AgeKind.MINOR) {
            if (birthMonth == null || birthYear == null) {
                return "redirect:/join";
            }
            birth = YearMonth.of(birthYear, birthMonth);
            // The floor holds even against an edited form (§3.3).
            if (!AgeRules.reached(16, birth, YearMonth.now(clock))) {
                response.addCookie(ageHold(request));
                return "join-refused";
            }
        }
        MagicLinkService.Outcome outcome =
                magicLinks.requestJoin(email.trim(), ageKind, birth, request.getRemoteAddr());
        if (outcome == MagicLinkService.Outcome.SENT) {
            return "link-sent";
        }
        model.addAttribute("ageKind", ageKind);
        model.addAttribute("birthMonth", birthMonth);
        model.addAttribute("birthYear", birthYear);
        model.addAttribute("error", emailError(outcome));
        return "join-email";
    }

    static String emailError(MagicLinkService.Outcome outcome) {
        return switch (outcome) {
            case INVALID_EMAIL -> "That doesn't look like an email address.";
            case BLOCKED_DOMAIN -> "That provider's inboxes are public — anyone could open this "
                    + "account. Docket blocks them as a security rule; private aliases "
                    + "(Hide My Email, SimpleLogin and the like) work fine.";
            case RATE_LIMITED -> "Too many link requests for now. Wait a while and try again.";
            case SENT -> null;
        };
    }

    private boolean held(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies != null && Arrays.stream(cookies)
                .anyMatch(c -> AGE_HOLD_COOKIE.equals(c.getName()));
    }

    private Cookie ageHold(HttpServletRequest request) {
        Cookie cookie = new Cookie(AGE_HOLD_COOKIE, "held");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/join");
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
