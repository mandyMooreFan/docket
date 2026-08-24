package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The autocomplete behind adding a Position (§6.1): reuse-first, so an existing
 * employer is picked rather than forked. Member-gated — it serves the edit form,
 * and §8.5 wants no free enumeration surfaces.
 */
@Controller
class CompanyOptionsController {

    private final Companies companies;

    CompanyOptionsController(Companies companies) {
        this.companies = companies;
    }

    /** htmx sends the field it triggers from ("company"); tests and links use "q". */
    @GetMapping("/companies/options")
    String options(@RequestParam(defaultValue = "") String q,
                   @RequestParam(defaultValue = "") String company,
                   HttpServletRequest request, Model model) {
        if (CurrentMember.get(request).isEmpty()) {
            return "redirect:/login";
        }
        String prefix = q.isBlank() ? company : q;
        model.addAttribute("names",
                prefix.isBlank() ? List.of() : companies.namesStartingWith(prefix, 8));
        return "fragments/company-options";
    }
}
