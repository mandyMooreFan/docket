package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
class CompanyPageController {

    private final Companies companies;
    private final CompanyPageService pages;

    CompanyPageController(Companies companies, CompanyPageService pages) {
        this.companies = companies;
        this.pages = pages;
    }

    /**
     * The Company page (§6.1). An absorbed Company's URL is not broken by a merge —
     * it redirects to the one entity it now is (§10.5).
     */
    @GetMapping("/companies/{id}")
    String page(@PathVariable long id,
                @RequestParam(required = false) String verification,
                HttpServletRequest request, Model model) {
        Company company = companies.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (company.merged()) {
            return "redirect:/companies/" + companies.resolved(company).id();
        }
        model.addAttribute("company", pages.pageFor(company, CurrentMember.get(request)));
        model.addAttribute("verification", verification);
        return "company";
    }
}
