package com.mbeebe.docket.moderation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The periodic public transparency log (§10.3): Reports received and actioned, by
 * category, no names.
 *
 * <p>"No names" is structural rather than careful. The counts come back from the
 * database already grouped, through a projection that has no column for a member, so
 * there is no point in this class where a name exists to be leaked by a template
 * change. §15.5 records that this log is forever.
 *
 * <p>Dismissals are counted beside upholdings on purpose. A log that published only
 * actions taken would flatter the moderator and tell the reader nothing about how often
 * a report was found to be wrong — which is the more interesting number, and the one a
 * reader has no other way to obtain.
 */
@Service
class TransparencyLog {

    private final ReportRepository reports;
    private final ModerationActionRepository actions;

    TransparencyLog(ReportRepository reports, ModerationActionRepository actions) {
        this.reports = reports;
        this.actions = actions;
    }

    @Transactional(readOnly = true)
    TransparencyPage since(Instant since) {
        List<TransparencyPage.Row> rows = reports.countsByCategorySince(since).stream()
                .map(count -> new TransparencyPage.Row(count.getCategory(), count.getReceived(),
                        count.getUpheld(), count.getDismissed()))
                .toList();
        return new TransparencyPage(
                rows,
                rows.stream().mapToLong(TransparencyPage.Row::received).sum(),
                actions.countByActedAtAfterAndKind(since, ModerationAction.Kind.REMOVAL),
                actions.countByActedAtAfterAndKind(since, ModerationAction.Kind.WITHDRAWAL),
                actions.countByActedAtAfterAndKind(since, ModerationAction.Kind.SUSPENSION),
                actions.countByActedAtAfterAndKind(since, ModerationAction.Kind.TERMINATION));
    }

    /** §15.5: the log is forever, so the default view is everything there has ever been. */
    @Transactional(readOnly = true)
    TransparencyPage everything() {
        return since(Instant.EPOCH);
    }
}
