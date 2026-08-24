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
import java.util.List;
import java.util.Set;

/**
 * §6.5's saved-search sender, in AgeRollover's clock-injected shape. Contents
 * are limited to matching postings since the last send — the same filter logic
 * as the board, evaluated at send time (including "roles where I know someone",
 * which is a fact about the graph as it stands today). No matches means no
 * mail at all, never an empty one, and the last-send mark only advances when a
 * mail actually went — so nothing is ever skipped or repeated.
 */
@Component
class JobSearchMailer {

    private static final Logger log = LoggerFactory.getLogger(JobSearchMailer.class);

    private final JobSearchRepository searches;
    private final JobPostingRepository postings;
    private final JobService jobs;
    private final Companies companies;
    private final Members members;
    private final JobMails mails;
    private final Clock clock;

    JobSearchMailer(JobSearchRepository searches, JobPostingRepository postings,
                    JobService jobs, Companies companies, Members members, JobMails mails,
                    Clock clock) {
        this.searches = searches;
        this.postings = postings;
        this.jobs = jobs;
        this.companies = companies;
        this.members = members;
        this.mails = mails;
        this.clock = clock;
    }

    @Scheduled(cron = "0 40 * * * *")
    void hourly() {
        int sent = runDue(clock.instant());
        if (sent > 0) {
            log.info("Saved searches: {} digest(s) sent", sent);
        }
    }

    @Transactional
    int runDue(Instant now) {
        int sent = 0;
        List<JobPosting> open = postings.openAt(now);
        for (JobSearch search : searches.findByStoppedAtIsNull()) {
            if (!search.due(now)) {
                continue;
            }
            Set<Long> known = search.knownOnly()
                    ? jobs.companiesWhereIKnowSomeone(search.memberId())
                    : null;
            List<JobPosting> matching = open.stream()
                    .filter(posting -> posting.postedAt().isAfter(search.sentUpTo()))
                    .filter(posting -> jobs.matches(posting, search.filters(), known))
                    .toList();
            if (matching.isEmpty()) {
                continue;
            }
            List<JobMails.DigestEntry> entries = matching.stream()
                    .map(posting -> new JobMails.DigestEntry(posting.id(),
                            "%s at %s — %s".formatted(posting.title(),
                                    companies.findResolved(posting.companyId())
                                            .map(Company::name).orElse(""),
                                    Salaries.line(posting.salaryMin(), posting.salaryMax(),
                                            posting.currency()))))
                    .toList();
            var member = members.find(search.memberId());
            if (member.isPresent()) {
                mails.savedSearch(member.get().email(), entries, search.stopToken());
                search.markSent(now);
                sent++;
            }
        }
        return sent;
    }
}
