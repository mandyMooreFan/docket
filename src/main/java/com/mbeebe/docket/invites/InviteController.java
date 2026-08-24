package com.mbeebe.docket.invites;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * The Invite page (§13.3), and the one destination §13.4's feed copy points at
 * when it says "or invite them".
 *
 * <p>A send that was accepted always answers the same way — one redirect, one
 * sentence — whether the mail went or was silently withheld from an address that
 * already has an account (§8.3). The refusals that do differ are all facts about
 * what the sender typed, never about who is on the far end.
 */
@Controller
class InviteController {

    private final InviteService invites;

    InviteController(InviteService invites) {
        this.invites = invites;
    }

    @GetMapping("/invite")
    String page(@RequestParam(required = false) String sent,
                HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("invite", invites.pageFor(member.get().id(), sent != null, null));
        return "invite";
    }

    @PostMapping("/invite")
    String send(@RequestParam(defaultValue = "") String email,
                @RequestParam(defaultValue = "") String note,
                HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (!invites.mayInvite(member.get().id())) {
            // §3.2's shape everywhere else in the product: a 403 reporting the
            // same check the page itself would have shown.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Inviting opens when your profile is complete.");
        }
        InviteService.Outcome outcome = invites.send(member.get(), email, note);
        if (outcome == InviteService.Outcome.SENT) {
            return "redirect:/invite?sent";
        }
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        model.addAttribute("invite", invites.pageFor(member.get().id(), false,
                InviteService.messageFor(outcome)));
        return "invite";
    }
}
