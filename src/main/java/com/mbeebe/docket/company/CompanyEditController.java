package com.mbeebe.docket.company;

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

import java.util.Optional;

/**
 * Editing a Company page (§6.1): anyone past the trust gate, all equally — there is
 * no owner and no hierarchy. Refusal is a 403, not a 404: the page exists and the
 * member may read it; what they lack is the derived capability.
 */
@Controller
class CompanyEditController {

    /** What the edit form shows — a view model, never the entity (§14.2). */
    record EditForm(long id, String name, String description, Long logoImageId) {
    }

    private final Companies companies;
    private final TrustGate trustGate;
    private final CompanyEditingService editing;

    CompanyEditController(Companies companies, TrustGate trustGate,
                          CompanyEditingService editing) {
        this.companies = companies;
        this.trustGate = trustGate;
        this.editing = editing;
    }

    @GetMapping("/companies/{id}/edit")
    String form(@PathVariable long id, HttpServletRequest request, Model model) {
        Company company = gated(id, request);
        model.addAttribute("companyEdit", new EditForm(company.id(), company.name(),
                company.description(), company.logoImageId()));
        return "company-edit";
    }

    @PostMapping("/companies/{id}/edit")
    String edit(@PathVariable long id, @RequestParam String name,
                @RequestParam String description, HttpServletRequest request,
                HttpServletResponse response, Model model) {
        Company company = gated(id, request);
        long editorId = CurrentMember.get(request).orElseThrow().id();
        return switch (editing.edit(company.id(), editorId, name, description)) {
            case SAVED -> "redirect:/companies/" + company.id();
            case BLANK_NAME -> reshow(company, name, description, response, model,
                    "A company needs a name.");
            case NAME_TAKEN -> reshow(company, name, description, response, model,
                    "Another company already has that name. If they're really the same "
                            + "employer, that's a merge for moderation, not a rename.");
        };
    }

    @GetMapping("/companies/{id}/history")
    String history(@PathVariable long id, HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        Company company = companies.findResolved(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("companyName", company.name());
        model.addAttribute("companyId", company.id());
        model.addAttribute("entries", editing.history(company.id(), member));
        return "company-history";
    }

    /** Resolve the Company and hold the door: sign-in first, then the trust gate. */
    private Company gated(long id, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            throw new RedirectToLogin();
        }
        Company company = companies.findResolved(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!trustGate.passes(member.get().id(), company.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return company;
    }

    private String reshow(Company company, String name, String description,
                          HttpServletResponse response, Model model, String error) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        model.addAttribute("companyEdit", new EditForm(company.id(), name, description,
                company.logoImageId()));
        model.addAttribute("error", error);
        return "company-edit";
    }

    /** Signed-out members are sent to sign in, from whichever door they knocked on. */
    static class RedirectToLogin extends RuntimeException {
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(RedirectToLogin.class)
    String toLogin() {
        return "redirect:/login";
    }
}
