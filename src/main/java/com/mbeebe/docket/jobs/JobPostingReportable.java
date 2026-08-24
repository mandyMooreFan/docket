package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.moderation.ReportableContent;
import com.mbeebe.docket.moderation.TargetKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The board's answer to {@link ReportableContent} (§10.2, §10.3): the JOB_POSTING
 * kind, and only that one.
 *
 * <p>A posting's ordinary visibility rule is the plainest in the product — §8.4
 * puts the board on the open web, so anyone who can reach the page can report
 * what is on it, signed in or out. What {@link #visibleToReporter} therefore
 * checks is the one thing that does hide a posting: §10.3's own removal. A
 * <em>closed</em> posting stays reportable, deliberately: it still renders, and a
 * Report about the salary range it advertised does not expire with the window.
 */
@Component
class JobPostingReportable implements ReportableContent {

    private final JobPostingRepository postings;
    private final Companies companies;

    JobPostingReportable(JobPostingRepository postings, Companies companies) {
        this.postings = postings;
        this.companies = companies;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> visibleToReporter(TargetKind kind, long id,
                                                    Optional<Member> viewer) {
        return kind == TargetKind.JOB_POSTING
                ? postings.findById(id).filter(posting -> !posting.removed()).map(this::item)
                : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> forModeration(TargetKind kind, long id) {
        return kind == TargetKind.JOB_POSTING
                ? postings.findById(id).map(this::item)
                : Optional.empty();
    }

    @Override
    @Transactional
    public boolean remove(TargetKind kind, long id, Instant now) {
        if (kind != TargetKind.JOB_POSTING) {
            return false;
        }
        postings.findById(id).ifPresent(posting -> posting.remove(now));
        return true;
    }

    @Override
    @Transactional
    public boolean restore(TargetKind kind, long id) {
        if (kind != TargetKind.JOB_POSTING) {
            return false;
        }
        postings.findById(id).ifPresent(JobPosting::restore);
        return true;
    }

    /**
     * §6.3: a posting is authored by a Member and never by the Company, so the
     * poster is the one answerable for it — the ladder's member-facing rungs have
     * someone to apply to.
     */
    private ReportedItem item(JobPosting posting) {
        String company = companies.findResolved(posting.companyId())
                .map(Company::name).orElse("");
        String summary = posting.title() + " at " + company + " — "
                + Salaries.line(posting.salaryMin(), posting.salaryMax(), posting.currency())
                + ", " + JobService.placeLine(posting) + "\n\n" + posting.description();
        return new ReportedItem(TargetKind.JOB_POSTING, posting.id(),
                Optional.of(posting.posterId()), summary, "/jobs/" + posting.id(),
                posting.removed());
    }
}
