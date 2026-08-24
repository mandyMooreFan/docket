package com.mbeebe.docket.search;

import java.util.List;

/**
 * What search asks the jobs board (§8.4: jobs are searchable logged out — the
 * lurker doing job research is a legitimate user, and requiring an account to
 * look for work would betray the board's founding rule). Implemented in
 * com.mbeebe.docket.jobs.
 *
 * <p>OPEN postings only, derived from the window against the clock at the moment
 * of asking, exactly as every other open/closed answer is (ADR-0002): the search
 * stops answering the instant §6.3's window closes, with no sweep in between.
 */
public interface PostingSearch {

    List<Hit> matching(String tsquery, int limit);

    record Hit(long id, String title, long companyId, String company, String salaryLine,
               String placeLine) {
    }
}
