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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * The poster's queue (§6.4) and the Application-scoped full-Profile view
 * (§6.3). Every route here is the posting author's alone — for anyone else the
 * pages do not exist, with no placeholder.
 *
 * <p>The one contact affordance in the queue is the "Message" link on a row:
 * the Application-scoped Thread (§7.1), shown only while that Application's
 * channel is open and reaching nobody who did not apply. There is no other way
 * to contact anyone from here, and messaging never mails — an "advanced"
 * Outcome deliberately sends no mail, so the poster's actual words arrive only
 * in the applicant's inbox (§7.4's one accepted cost).
 */
@Controller
class QueueController {

    private final JobService jobs;

    QueueController(JobService jobs) {
        this.jobs = jobs;
    }

    @GetMapping("/jobs/{id}/applications")
    String queue(@PathVariable long id, HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("queue", jobs.queueFor(id, member.get())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "job-applications";
    }

    @PostMapping("/jobs/{postingId}/applications/{applicationId}/advance")
    String advance(@PathVariable long postingId, @PathVariable long applicationId,
                   HttpServletRequest request) {
        return resolve(postingId, applicationId, request, JobApplication.Outcome.ADVANCED);
    }

    @PostMapping("/jobs/{postingId}/applications/{applicationId}/not-select")
    String notSelect(@PathVariable long postingId, @PathVariable long applicationId,
                     HttpServletRequest request) {
        return resolve(postingId, applicationId, request, JobApplication.Outcome.NOT_SELECTED);
    }

    @GetMapping("/jobs/{postingId}/applications/{applicationId}/profile")
    String applicationProfile(@PathVariable long postingId, @PathVariable long applicationId,
                              HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        JobService.ApplicationProfile view =
                jobs.applicationProfile(postingId, applicationId, member.get())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("view", view);
        model.addAttribute("profile", view.profile());
        return "job-application-profile";
    }

    private String resolve(long postingId, long applicationId, HttpServletRequest request,
                           JobApplication.Outcome outcome) {
        Member member = requireMember(request);
        if (!jobs.resolve(member, postingId, applicationId, outcome)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/jobs/" + postingId + "/applications";
    }

    private Member requireMember(HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        return member.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
