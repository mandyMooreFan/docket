package com.mbeebe.docket.jobs;

import java.util.List;

/**
 * The board as anyone may see it (§6.5, §8.4): one list, newest first, no
 * ranking, no personalisation — the rows are the same for everyone, only the
 * affordances around them differ by being signed in.
 */
public record BoardPage(List<PostingRow> rows, JobFilters filters, boolean signedIn,
                        List<SavedSearchRow> savedSearches) {

    /** §13.4's copy renders only for the truly empty board, not a narrow filter. */
    public boolean emptyBoard() {
        return rows.isEmpty() && !filters.any();
    }

    public boolean emptyMatch() {
        return rows.isEmpty() && filters.any();
    }

    /** One of the member's saved searches (§6.5), listed for a one-click stop. */
    public record SavedSearchRow(long id, String describes, String frequency) {
    }
}
