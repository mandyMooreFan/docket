package com.mbeebe.docket.jobs;

import com.mbeebe.docket.feed.JobAttachedPosts;
import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
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
 * The board (§6.5, §8.4: browsable logged out, filters as GET params), the
 * posting page, and posting itself (§6.3). Every gate refusal is a 403 whose
 * message reports the same check the page would have shown (§3.2).
 */
@Controller
class JobsController {

    private final JobService jobs;
    private final JobAttachedPosts attachedPosts;
    private final CapabilityService capabilities;

    JobsController(JobService jobs, JobAttachedPosts attachedPosts,
                   CapabilityService capabilities) {
        this.jobs = jobs;
        this.attachedPosts = attachedPosts;
        this.capabilities = capabilities;
    }

    @GetMapping("/jobs")
    String board(@RequestParam(defaultValue = "") String q,
                 @RequestParam(defaultValue = "") String location,
                 @RequestParam(defaultValue = "") String remote,
                 @RequestParam(defaultValue = "") String floor,
                 @RequestParam(defaultValue = "GBP") String currency,
                 @RequestParam(defaultValue = "") String company,
                 @RequestParam(defaultValue = "") String known,
                 HttpServletRequest request, Model model) {
        Optional<Member> viewer = CurrentMember.get(request);
        JobFilters filters = new JobFilters(q.strip(), location.strip(), remote.strip(),
                floor.strip(), currency.strip(), company.strip(), !known.isBlank());
        model.addAttribute("board", jobs.board(filters, viewer));
        return "jobs";
    }

    /** The posting form — or, for a member the gate refuses, the honest why (§3.2). */
    @GetMapping("/jobs/new")
    String newPosting(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("companies", jobs.postableCompanies(member.get().id()));
        model.addAttribute("blockReason", jobs.blockReason(member.get().id()).orElse(null));
        return "job-new";
    }

    @PostMapping("/jobs")
    String post(@RequestParam(defaultValue = "") String companyId,
                @RequestParam(defaultValue = "") String title,
                @RequestParam(defaultValue = "") String location,
                @RequestParam(defaultValue = "") String remotePolicy,
                @RequestParam(defaultValue = "") String salaryMin,
                @RequestParam(defaultValue = "") String salaryMax,
                @RequestParam(defaultValue = "") String currency,
                @RequestParam(defaultValue = "") String description,
                HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        try {
            long id = jobs.post(member.get(), companyId, title, location, remotePolicy,
                    salaryMin, salaryMax, currency, description);
            return "redirect:/jobs/" + id;
        } catch (JobService.NotAllowed refused) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, refused.getMessage());
        } catch (JobService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("companies", jobs.postableCompanies(member.get().id()));
            model.addAttribute("blockReason", null);
            model.addAttribute("error", refused.getMessage());
            return "job-new";
        }
    }

    @GetMapping("/jobs/{id}")
    String posting(@PathVariable long id, HttpServletRequest request, Model model) {
        model.addAttribute("posting", jobs.postingPage(id, CurrentMember.get(request))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "job";
    }

    /** §6.3: one click plus an optional note — the Profile is the Application. */
    @PostMapping("/jobs/{id}/apply")
    String apply(@PathVariable long id, @RequestParam(defaultValue = "") String note,
                 HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        try {
            jobs.apply(member.get(), id, note);
            return "redirect:/jobs/" + id;
        } catch (java.util.NoSuchElementException gone) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (JobService.NotAllowed refused) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, refused.getMessage());
        } catch (JobService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("posting", jobs.postingPage(id, member)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
            model.addAttribute("error", refused.getMessage());
            return "job";
        }
    }

    /**
     * §5.2.2: a member writes a Post and attaches this posting — the ONLY path
     * from board to feed. A job-attached Post is still a Post (§3.2's POST).
     */
    @PostMapping("/jobs/{id}/share")
    String share(@PathVariable long id, @RequestParam(defaultValue = "") String body,
                 HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (capabilities.may(member.get().id(), Capability.POST) != CapabilityAnswer.YES) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Posting opens when your profile is complete.");
        }
        try {
            return "redirect:/posts/" + attachedPosts.compose(member.get(), id, body);
        } catch (JobAttachedPosts.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("posting", jobs.postingPage(id, member)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
            model.addAttribute("shareError", refused.getMessage());
            return "job";
        }
    }

    /** §6.4: the applicant can always see their Applications' states. */
    @GetMapping("/applications")
    String mine(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("rows", jobs.applicationsOf(member.get().id()));
        return "applications";
    }
}
