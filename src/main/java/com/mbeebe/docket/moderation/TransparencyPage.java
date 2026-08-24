package com.mbeebe.docket.moderation;

import java.util.List;

/**
 * The public transparency log (§10.3), as the page renders it: counts by category and
 * counts of what was done, and nothing else.
 *
 * <p>The type itself is the "no names" guarantee. There is no field here that could
 * hold a Member, a reporter or an email address, so no template change can leak one —
 * the counts arrive already grouped from the database and nothing member-shaped is
 * loaded to produce this page.
 */
public record TransparencyPage(List<Row> rows,
                               long received,
                               long removals,
                               long withdrawals,
                               long suspensions,
                               long terminations) {

    public boolean empty() {
        return received == 0;
    }

    /** One category's row. Counts only — the type cannot carry a name. */
    public record Row(ReportCategory category, long received, long upheld, long dismissed) {
    }
}
