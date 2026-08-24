package com.mbeebe.docket.moderation;

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

/**
 * The public transparency log (§10.3, §15.5), and the owner's two remaining queues.
 *
 * <p>The log is public and requires no account, because a log only the moderated can
 * read is not transparency. It carries counts by category and nothing else — the
 * projection behind it has no column for a name, so there is nothing here for a
 * template change to leak.
 */
@Controller
class TransparencyController {

    private final TransparencyLog log;
    private final IntimateImageService intimateImages;
    private final ComplaintService complaints;
    private final Owner owner;

    TransparencyController(TransparencyLog log, IntimateImageService intimateImages,
                           ComplaintService complaints, Owner owner) {
        this.log = log;
        this.intimateImages = intimateImages;
        this.complaints = complaints;
        this.owner = owner;
    }

    @GetMapping("/transparency")
    String transparency(Model model) {
        model.addAttribute("log", log.everything());
        return "transparency";
    }

    @GetMapping("/moderation/intimate-images")
    String intimateImageQueue(HttpServletRequest request, Model model) {
        requireOwner(request);
        model.addAttribute("reports", intimateImages.queue());
        return "moderation-intimate-images";
    }

    @PostMapping("/moderation/intimate-images/{id}")
    String decideIntimateImage(@PathVariable long id, @RequestParam String decision,
                               @RequestParam String reason, HttpServletRequest request) {
        requireOwner(request);
        if ("CONFIRMED".equals(decision)) {
            intimateImages.confirm(id, reason);
        } else {
            intimateImages.restore(id, reason);
        }
        return "redirect:/moderation/intimate-images";
    }

    @GetMapping("/moderation/complaints")
    String complaintQueue(HttpServletRequest request, Model model) {
        requireOwner(request);
        model.addAttribute("complaints", complaints.open());
        return "moderation-complaints";
    }

    @PostMapping("/moderation/complaints/{id}")
    String respond(@PathVariable long id, @RequestParam String response,
                   HttpServletRequest request) {
        requireOwner(request);
        complaints.respond(id, response);
        return "redirect:/moderation/complaints";
    }

    private Member requireOwner(HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (!owner.is(member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return member.orElseThrow();
    }
}
