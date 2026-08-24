package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.Images;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
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
    private final Images images;

    CompanyEditController(Companies companies, TrustGate trustGate,
                          CompanyEditingService editing, Images images) {
        this.companies = companies;
        this.trustGate = trustGate;
        this.editing = editing;
        this.images = images;
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

    /**
     * The product's first image upload (§10.4): the bytes go through the images
     * module's checked store — a refused image never existed here.
     */
    @PostMapping("/companies/{id}/logo")
    String logo(@PathVariable long id, @RequestParam("logo") MultipartFile logo,
                HttpServletRequest request, HttpServletResponse response, Model model) {
        Company company = gated(id, request);
        long editorId = CurrentMember.get(request).orElseThrow().id();
        byte[] bytes;
        try {
            bytes = logo.getBytes();
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Images.Stored stored = images.store(bytes, logo.getContentType());
        return switch (stored.outcome()) {
            case STORED -> {
                editing.setLogo(company.id(), editorId, stored.imageId());
                yield "redirect:/companies/" + company.id();
            }
            case WRONG_TYPE -> reshow(company, company.name(), company.description(),
                    response, model, "A logo is a PNG or a JPEG.");
            case TOO_LARGE -> reshow(company, company.name(), company.description(),
                    response, model, "Logos are capped at 512 KB.");
            case REFUSED -> reshow(company, company.name(), company.description(),
                    response, model, "That image was refused by the upload checks.");
        };
    }

    @GetMapping("/companies/{id}/history")
    String history(@PathVariable long id, HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        Company company = companies.findResolved(id)
                .filter(found -> !found.removed())
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
        // §10.3: a removed Company is a 404 on every one of its doors, edit
        // included — there is no page left to vandalise or to defend.
        Company company = companies.findResolved(id)
                .filter(found -> !found.removed())
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
