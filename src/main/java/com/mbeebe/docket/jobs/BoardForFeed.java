package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.feed.JobBoardLookup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The board's answer to {@link JobBoardLookup}: the compact card a
 * job-attached Post renders (§5.2.2), and the rail's "Jobs from your network"
 * (§2.3) — the same companies-where-I-know-someone fact as the board filter,
 * shown newest first, a handful at most, never ranked.
 */
@Component
class BoardForFeed implements JobBoardLookup {

    /** A rail panel is a glance, not a second board. */
    private static final int RAIL_LIMIT = 5;

    private final JobPostingRepository postings;
    private final JobService jobs;
    private final Companies companies;
    private final Clock clock;

    BoardForFeed(JobPostingRepository postings, JobService jobs, Companies companies,
                 Clock clock) {
        this.postings = postings;
        this.jobs = jobs;
        this.companies = companies;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttachedPosting> attached(long postingId) {
        return postings.findById(postingId).map(this::card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachedPosting> openAtConnectedCompanies(long memberId) {
        Set<Long> known = jobs.companiesWhereIKnowSomeone(memberId);
        if (known.isEmpty()) {
            return List.of();
        }
        return postings.openAt(clock.instant()).stream()
                .filter(posting -> known.contains(posting.companyId()))
                .limit(RAIL_LIMIT)
                .map(this::card)
                .toList();
    }

    private AttachedPosting card(JobPosting posting) {
        return new AttachedPosting(posting.id(), posting.title(),
                companies.findResolved(posting.companyId()).map(Company::name).orElse(""),
                Salaries.line(posting.salaryMin(), posting.salaryMax(), posting.currency()),
                posting.openAt(clock.instant()));
    }
}
