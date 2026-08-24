package com.mbeebe.docket.jobs;

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
 * Saved searches (§6.5): explicit creation from the board's current filter
 * set, and the two ways to stop — the board's own button, and the mail's
 * tokenized one-click link, which deliberately works signed out (the person
 * standing in their inbox is the person the mail bothers).
 */
@Controller
class JobSearchController {

    private final JobSearchService searches;

    JobSearchController(JobSearchService searches) {
        this.searches = searches;
    }

    @PostMapping("/jobs/searches")
    String create(@RequestParam(defaultValue = "") String q,
                  @RequestParam(defaultValue = "") String location,
                  @RequestParam(defaultValue = "") String remote,
                  @RequestParam(defaultValue = "") String floor,
                  @RequestParam(defaultValue = "GBP") String currency,
                  @RequestParam(defaultValue = "") String company,
                  @RequestParam(defaultValue = "") String known,
                  @RequestParam(defaultValue = "WEEKLY") String frequency,
                  HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        searches.create(member.get(), new JobFilters(q.strip(), location.strip(),
                remote.strip(), floor.strip(), currency.strip(), company.strip(),
                !known.isBlank()), frequency);
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/searches/{id}/stop")
    String stopOwn(@PathVariable long id, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (!searches.stopOwn(member.get(), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/jobs";
    }

    /** §6.5: one click, no login — the click IS the stop. */
    @GetMapping("/jobs/searches/stop/{token}")
    String stopByToken(@PathVariable String token, Model model) {
        model.addAttribute("stopped", searches.stopByToken(token));
        return "search-stopped";
    }
}
