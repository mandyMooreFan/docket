package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.identity.Members;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * §6.4's automatic close, in AgeRollover's shape: a clock-injected sweep. A
 * posting past its window edge is already closed to every read (derived,
 * ADR-0002) — this job records the dated fact and executes the guarantee:
 * every untouched Application becomes "closed without response", and each of
 * those applicants is told by mail (§6.5). Hourly, because the window edge is
 * a moment, not a date.
 */
@Component
class PostingCloser {

    private static final Logger log = LoggerFactory.getLogger(PostingCloser.class);

    private final JobPostingRepository postings;
    private final JobApplicationRepository applications;
    private final Companies companies;
    private final Members members;
    private final JobMails mails;
    private final Clock clock;

    PostingCloser(JobPostingRepository postings, JobApplicationRepository applications,
                  Companies companies, Members members, JobMails mails, Clock clock) {
        this.postings = postings;
        this.applications = applications;
        this.companies = companies;
        this.members = members;
        this.mails = mails;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 * * * *")
    void hourly() {
        int closed = closeDue(clock.instant());
        if (closed > 0) {
            log.info("Posting close: {} posting(s) closed, queues resolved to facts", closed);
        }
    }

    @Transactional
    int closeDue(Instant now) {
        int closed = 0;
        for (JobPosting posting : postings.dueToClose(now)) {
            posting.markClosed(now);
            String companyName = companies.findResolved(posting.companyId())
                    .map(Company::name).orElse("");
            for (JobApplication application
                    : applications.findByPostingIdOrderByAppliedAtAscIdAsc(posting.id())) {
                if (application.unresolved() && application.closedWithoutResponseAt() == null) {
                    application.closeWithoutResponse(now);
                    // §6.4: "the applicant is told" — the transactional mail (§6.5).
                    members.find(application.applicantId()).ifPresent(applicant ->
                            mails.closedWithoutResponse(applicant.email(), posting, companyName));
                }
            }
            closed++;
        }
        return closed;
    }
}
