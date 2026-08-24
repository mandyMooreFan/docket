package com.mbeebe.docket.moderation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The two statutory routes that take no account (§10.5, §11.3).
 *
 * <p>Both are deliberately reachable logged out, and both say on their face what they
 * are <em>not</em>, because the commonest failure of a form like this is a person
 * pouring the wrong thing into the nearest box. §10.5's route is not the general report
 * flow; §11.3's complaint is neither.
 *
 * <p>Neither form asks for an account, and that is the point rather than a convenience.
 * s.20A covers "users and affected persons", and the person depicted in an intimate
 * image is precisely the one least likely to be a Member; a data-protection complaint
 * may come from someone whose data was handled without their ever having joined.
 */
@Controller
class SafetyFormsController {

    private final IntimateImageService intimateImages;
    private final ComplaintService complaints;

    SafetyFormsController(IntimateImageService intimateImages, ComplaintService complaints) {
        this.intimateImages = intimateImages;
        this.complaints = complaints;
    }

    // ---- §10.5, OSA s.20A -------------------------------------------------------

    @GetMapping("/safety/intimate-image")
    String intimateImageForm() {
        return "intimate-image";
    }

    @PostMapping("/safety/intimate-image")
    String intimateImage(@RequestParam String locator,
                         @RequestParam(defaultValue = "false") boolean subjectDeclared,
                         @RequestParam(defaultValue = "false") boolean actingFor,
                         @RequestParam(defaultValue = "false") boolean goodFaith,
                         @RequestParam String contact,
                         HttpServletRequest request, Model model) {
        IntimateImageService.Outcome outcome = intimateImages.report(
                locator, subjectDeclared, actingFor, goodFaith, contact, request.getRemoteAddr());
        return switch (outcome) {
            case HELD, RECEIVED_UNRESOLVED -> {
                model.addAttribute("held", outcome == IntimateImageService.Outcome.HELD);
                yield "intimate-image-sent";
            }
            case INCOMPLETE -> {
                model.addAttribute("error", "The law requires all four of these: what and "
                        + "where, that you are the person shown or acting for them, that the "
                        + "report is made in good faith, and a way to reach you.");
                yield "intimate-image";
            }
            case RATE_LIMITED -> {
                model.addAttribute("error", "Too many reports from here for now. "
                        + "Wait a while and try again — and if this is urgent, the address "
                        + "on the safety page reaches a person.");
                yield "intimate-image";
            }
        };
    }

    // ---- §11.3, DUAA s.164A -----------------------------------------------------

    @GetMapping("/data-protection")
    String complaintForm() {
        return "data-protection";
    }

    @PostMapping("/data-protection")
    String complain(@RequestParam String contact, @RequestParam String account,
                    HttpServletRequest request, Model model) {
        return switch (complaints.complain(contact, account, request.getRemoteAddr())) {
            case ACKNOWLEDGED -> "data-protection-sent";
            case INCOMPLETE -> {
                model.addAttribute("error",
                        "We need both a way to reach you and what the complaint is.");
                yield "data-protection";
            }
            case RATE_LIMITED -> {
                model.addAttribute("error",
                        "Too many complaints from here for now. Wait a while and try again.");
                yield "data-protection";
            }
        };
    }
}
