package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.CompanyPostings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * The board's answer to {@link CompanyPostings}: the Company page's postings
 * section (§6.1), derived from the window against the clock like every other
 * open/closed answer (ADR-0002).
 */
@Component
class CompanyPostingsFromBoard implements CompanyPostings {

    private final JobPostingRepository postings;
    private final Clock clock;

    CompanyPostingsFromBoard(JobPostingRepository postings, Clock clock) {
        this.postings = postings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> openAt(long companyId) {
        return postings.openAtCompany(companyId, clock.instant()).stream()
                .map(posting -> new Entry(posting.id(), posting.title(),
                        Salaries.line(posting.salaryMin(), posting.salaryMax(),
                                posting.currency()),
                        JobService.placeLine(posting)))
                .toList();
    }
}
