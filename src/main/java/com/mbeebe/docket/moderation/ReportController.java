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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Reporting, in-product (§10.2), and the published address for everyone else.
 *
 * <p>A report form for something you cannot see does not exist: the route 404s rather
 * than refusing, because a form that said "you may not report this" would confirm the
 * item is there, and §8.5 does not allow the product to answer that question. This is
 * the same discipline the Profile page uses — 404, no placeholder.
 *
 * <p>§15.3's prominence requirement (Children's code Standard 15) is met by placement:
 * every page carries the Safety link in the app bar, and the reporting route is one
 * click from it. That is placement, not new capability, exactly as the standard says.
 */
@Controller
class ReportController {

    private final ModerationService moderation;
    private final ReportableContents content;
    private final Owner owner;

    ReportController(ModerationService moderation, ReportableContents content, Owner owner) {
        this.moderation = moderation;
        this.content = content;
        this.owner = owner;
    }

    /** The safety page: how reporting works, what the routes are, and who reviews. */
    @GetMapping("/safety")
    String safety(Model model) {
        model.addAttribute("publishedAddress", owner.publishedAddress().orElse(null));
        return "safety";
    }

    /** The member conduct policy (§10.6) — six items, and everything else is not an offence. */
    @GetMapping("/conduct")
    String conduct(Model model) {
        model.addAttribute("categories", List.of(ReportCategory.values()));
        model.addAttribute("publishedAddress", owner.publishedAddress().orElse(null));
        return "conduct";
    }

    @GetMapping("/report/{kind}/{id}")
    String form(@PathVariable TargetKind kind, @PathVariable long id,
                HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("item", visibleOr404(kind, id, member));
        model.addAttribute("kind", kind);
        model.addAttribute("targetId", id);
        model.addAttribute("categories", List.of(ReportCategory.values()));
        return "report";
    }

    @PostMapping("/report/{kind}/{id}")
    String submit(@PathVariable TargetKind kind, @PathVariable long id,
                  @RequestParam ReportCategory category,
                  @RequestParam String account,
                  HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        try {
            return moderation.report(member.get(), kind, id, category, account)
                    .map(reportId -> "redirect:/report/sent")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } catch (ModerationService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("item", visibleOr404(kind, id, member));
            model.addAttribute("kind", kind);
            model.addAttribute("targetId", id);
            model.addAttribute("categories", List.of(ReportCategory.values()));
            model.addAttribute("error", refused.getMessage());
            return "report";
        }
    }

    @GetMapping("/report/sent")
    String sent() {
        return "report-sent";
    }

    private ReportableContent.ReportedItem visibleOr404(TargetKind kind, long id,
                                                        Optional<Member> viewer) {
        return content.visibleToReporter(kind, id, viewer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
