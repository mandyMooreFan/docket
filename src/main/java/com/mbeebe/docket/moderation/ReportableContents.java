package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Member;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Every {@link ReportableContent} contributor in the application, asked in turn.
 *
 * <p>The default is the load-bearing line, and it points the opposite way from {@code
 * ImageAudiences}': no claim means <em>not reportable</em> and <em>not removable</em>.
 * A kind nobody owns cannot be reported (the route 404s) and cannot be removed (the
 * ladder refuses rather than silently succeeding). A module that starts publishing
 * something reportable and forgets to contribute gets a visible hole in the queue,
 * which a test catches; the alternative default would have got it a rung that reports
 * success while removing nothing.
 */
@Component
class ReportableContents {

    // Resolved per call, not at construction: a contributor lives in the module that
    // owns the rows, and those modules read moderation's own standing service for their
    // gates — eager injection would close a cycle at startup. Asking at read time is
    // also the honest shape, since every answer is derived per request anyway.
    private final ObjectProvider<ReportableContent> contributors;

    ReportableContents(ObjectProvider<ReportableContent> contributors) {
        this.contributors = contributors;
    }

    Optional<ReportableContent.ReportedItem> visibleToReporter(TargetKind kind, long id,
                                                               Optional<Member> viewer) {
        return contributors.orderedStream()
                .flatMap(contributor -> contributor.visibleToReporter(kind, id, viewer).stream())
                .findFirst();
    }

    Optional<ReportableContent.ReportedItem> forModeration(TargetKind kind, long id) {
        return contributors.orderedStream()
                .flatMap(contributor -> contributor.forModeration(kind, id).stream())
                .findFirst();
    }

    boolean remove(TargetKind kind, long id, Instant now) {
        return contributors.orderedStream()
                .anyMatch(contributor -> contributor.remove(kind, id, now));
    }

    boolean restore(TargetKind kind, long id) {
        return contributors.orderedStream()
                .anyMatch(contributor -> contributor.restore(kind, id));
    }
}
