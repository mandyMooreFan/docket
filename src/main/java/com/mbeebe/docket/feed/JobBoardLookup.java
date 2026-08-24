package com.mbeebe.docket.feed;

import java.util.List;
import java.util.Optional;

/**
 * What the feed asks of the jobs board (#35), one interface both ways of
 * looking: the compact card a job-attached Post renders (§5.2.2), and the
 * rail's "Jobs from your network" panel (§2.3) — open postings at companies
 * where the member has a Connection, derived and unranked. The same seam shape
 * as {@link com.mbeebe.docket.profile.ProfilePostsLookup}, implemented in
 * com.mbeebe.docket.jobs.
 */
public interface JobBoardLookup {

    /** The posting a Post attached, shaped for its card; empty renders no card. */
    Optional<AttachedPosting> attached(long postingId);

    /** §2.3's rail: open postings at companies where a Connection works. Newest first. */
    List<AttachedPosting> openAtConnectedCompanies(long memberId);

    record AttachedPosting(long id, String title, String company, String salaryLine,
                           boolean open) {
    }
}
