package com.mbeebe.docket.leaving;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Your data, and the door out (SPEC.md §11.1–§11.3): one page, one button, and the
 * deletion beneath it — in that order, because §11.2 says deletion "offers the
 * export first and never requires it".
 *
 * <p>The offer is the page's layout and nothing more. Nothing on the delete path
 * checks whether an archive was ever taken, and nothing ever will: making the
 * export a precondition would turn a right into a toll gate, which is the exact
 * shape of the "hindrance" the guidance forbids ({@code docs/data-rights.md} §6).
 */
@Controller
class LeavingController {

    private final Archive archive;
    private final Termination termination;
    private final SessionService sessions;

    LeavingController(Archive archive, Termination termination, SessionService sessions) {
        this.archive = archive;
        this.termination = termination;
        this.sessions = sessions;
    }

    @GetMapping("/settings/data")
    String page(HttpServletRequest request, Model model) {
        return CurrentMember.get(request)
                .map(member -> {
                    model.addAttribute("confirmed", true);
                    return "data";
                })
                .orElse("redirect:/login");
    }

    /**
     * The archive itself (§11.1).
     *
     * <p><strong>There is no member id in this URL, and that is the guard.</strong>
     * The route serves the signed-in member and has no way to be asked for anybody
     * else's archive: a second member hitting exactly this address gets their own,
     * because the only subject this method can name is {@code CurrentMember}. That
     * matters more here than on most routes — an archive carries image bytes that
     * /images/{id} would refuse a third party (§51's audience guard), so an
     * addressable archive would be a hole in that guard rather than a copy behind
     * it. Signed out is a plain 404, the discipline the rest of the product
     * applies to anything not yours to see, rather than a redirect that would
     * confirm the route exists.
     *
     * <p>Written straight to the response, never cached and never shared-cached:
     * this is the most concentrated personal data the product will ever emit.
     */
    @GetMapping("/settings/data/export.zip")
    void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Member member = CurrentMember.get(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"docket-export-" + member.id() + ".zip\"");
        response.setHeader("Cache-Control", "no-store, private");
        archive.writeTo(member, response.getOutputStream());
    }

    /**
     * §11.2, and the point of no return. The tick is not ceremony — the deletion
     * copy above it names what stays behind, and a member should have to have been
     * on the page where that is written before the account ends.
     */
    @PostMapping("/settings/data/leave")
    String leave(@RequestParam(defaultValue = "") String confirm, HttpServletRequest request,
                 HttpServletResponse response, Model model) {
        Member member = CurrentMember.get(request).orElse(null);
        if (member == null) {
            return "redirect:/login";
        }
        if (confirm.isBlank()) {
            model.addAttribute("confirmed", false);
            return "data";
        }
        termination.terminate(member, Termination.Reason.MEMBER_REQUEST);
        // Termination already destroyed every session; this clears the cookie that
        // is now pointing at nothing.
        sessions.signOut(request, response);
        return "redirect:/left";
    }

    /** The last honest word, and where §11.3's backups position is said again. */
    @GetMapping("/left")
    String left() {
        return "left";
    }
}
