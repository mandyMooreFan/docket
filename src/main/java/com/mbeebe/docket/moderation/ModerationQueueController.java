package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * The owner's reactive queue (§10.1) and the four rungs that answer it (§10.3).
 *
 * <p>Not the owner: 404, not 403. A queue that refused you by name would tell you it is
 * there and roughly how big the moderation surface is; the page simply does not exist
 * for anyone else, which is the same answer the product gives for a Profile you may not
 * see.
 */
@Controller
class ModerationQueueController {

    private final ModerationService moderation;
    private final Owner owner;

    ModerationQueueController(ModerationService moderation, Owner owner) {
        this.moderation = moderation;
        this.owner = owner;
    }

    @GetMapping("/moderation")
    String queue(HttpServletRequest request, Model model) {
        requireOwner(request);
        model.addAttribute("queue", moderation.queue());
        model.addAttribute("appeals", moderation.openAppeals());
        return "moderation-queue";
    }

    @GetMapping("/moderation/reports/{id}")
    String report(@PathVariable long id, HttpServletRequest request, Model model) {
        requireOwner(request);
        model.addAttribute("entry", moderation.queueEntry(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        model.addAttribute("capabilities", List.of(Capability.values()));
        return "moderation-report";
    }

    @PostMapping("/moderation/reports/{id}/dismiss")
    String dismiss(@PathVariable long id, @RequestParam String reason,
                   HttpServletRequest request) {
        moderation.dismiss(id, reason, requireOwner(request));
        return "redirect:/moderation";
    }

    @PostMapping("/moderation/reports/{id}/remove")
    String remove(@PathVariable long id, @RequestParam String reason,
                  HttpServletRequest request) {
        moderation.removeItem(id, reason, requireOwner(request));
        return "redirect:/moderation";
    }

    @PostMapping("/moderation/reports/{id}/withdraw")
    String withdraw(@PathVariable long id, @RequestParam Capability capability,
                    @RequestParam(required = false) Integer days,
                    @RequestParam String reason, HttpServletRequest request) {
        moderation.withdraw(id, capability, period(days), reason, requireOwner(request));
        return "redirect:/moderation";
    }

    @PostMapping("/moderation/reports/{id}/suspend")
    String suspend(@PathVariable long id, @RequestParam(required = false) Integer days,
                   @RequestParam String reason, HttpServletRequest request) {
        moderation.suspend(id, period(days), reason, requireOwner(request));
        return "redirect:/moderation";
    }

    @PostMapping("/moderation/reports/{id}/terminate")
    String terminate(@PathVariable long id, @RequestParam String reason,
                     HttpServletRequest request) {
        moderation.terminate(id, reason, requireOwner(request));
        return "redirect:/moderation";
    }

    @PostMapping("/moderation/appeals/{id}")
    String decideAppeal(@PathVariable long id, @RequestParam Appeal.Outcome outcome,
                        @RequestParam String reason, HttpServletRequest request) {
        requireOwner(request);
        moderation.decideAppeal(id, outcome, reason);
        return "redirect:/moderation";
    }

    /** Blank means indefinite, and the product says "indefinitely" rather than picking a date. */
    private Optional<Duration> period(Integer days) {
        return days == null || days <= 0 ? Optional.empty() : Optional.of(Duration.ofDays(days));
    }

    private Member requireOwner(HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (!owner.is(member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return member.orElseThrow();
    }

    @ExceptionHandler(ModerationService.Refused.class)
    ResponseStatusException refused(ModerationService.Refused refused) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, refused.getMessage());
    }
}
