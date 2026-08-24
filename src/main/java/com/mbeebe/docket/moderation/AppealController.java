package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * The one Appeal (§10.3), and the page where a Member is told where they stand.
 *
 * <p>This is the surface that carries §10.3's requirement that "a capability never
 * earned and a capability withdrawn are different states, and the member is told which
 * they are in". The never-earned case has no page here at all — the product tells you
 * to finish your Profile, at the gate, where it already did. Arriving here means
 * something was taken, and the page says what, why, until when, and by what route to
 * argue.
 *
 * <p>Reachable while suspended, deliberately: {@link ReadOnlyGuard} lets these POSTs
 * through, because a remedy a suspension swallowed would not be a remedy.
 */
@Controller
class AppealController {

    private final ModerationService moderation;
    private final MemberStanding standing;

    AppealController(ModerationService moderation, MemberStanding standing) {
        this.moderation = moderation;
        this.standing = standing;
    }

    /** Where you stand: every rung against you right now, with its reason. */
    @GetMapping("/appeals")
    String standing(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("notices", standing.noticesFor(member.get().id()));
        return "appeals";
    }

    @GetMapping("/appeals/{actionId}")
    String form(@PathVariable long actionId, HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("actionId", actionId);
        return "appeal";
    }

    @PostMapping("/appeals/{actionId}")
    String appeal(@PathVariable long actionId, @RequestParam String account,
                  HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        try {
            moderation.appeal(member.get(), actionId, account);
            return "redirect:/appeals?appealed";
        } catch (ModerationService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("actionId", actionId);
            model.addAttribute("error", refused.getMessage());
            return "appeal";
        }
    }
}
