package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.search.PostingSearch;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * The board's answer to {@link PostingSearch} (§8.4: jobs answer logged out).
 * Open postings only, derived from the window against the clock at the moment
 * of asking — the same single source of open/closed the board, the Company page
 * and the feed rail all read (ADR-0002).
 */
@Component
class PostingsByText implements PostingSearch {

    private final JobPostingRepository postings;
    private final Companies companies;
    private final Clock clock;

    PostingsByText(JobPostingRepository postings, Companies companies, Clock clock) {
        this.postings = postings;
        this.companies = companies;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hit> matching(String tsquery, int limit) {
        return postings.openMatchingRanked(tsquery, clock.instant(), limit).stream()
                .map(posting -> new Hit(posting.id(), posting.title(), posting.companyId(),
                        companies.findResolved(posting.companyId())
                                .map(Company::name).orElse(""),
                        Salaries.line(posting.salaryMin(), posting.salaryMax(),
                                posting.currency()),
                        JobService.placeLine(posting)))
                .toList();
    }
}
