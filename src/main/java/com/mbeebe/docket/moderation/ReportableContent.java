package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Member;

import java.time.Instant;
import java.util.Optional;

/**
 * How moderation reaches the six reportable kinds without reaching into six modules.
 *
 * <p>The inverted seam the product uses for every cross-module read (cf. {@code
 * ImageAudience}, {@code ConnectionLookup}): declared here, implemented by the module
 * that owns the rows. {@link Optional#empty()} means "not mine" — a contributor answers
 * only for the kinds it owns, and the registry falls through to the next.
 *
 * <p>Two lookups rather than one, because the two askers are not the same person. A
 * reporter may only report what they can already see, so {@link #visibleToReporter}
 * applies the ordinary visibility rules and §10.2's hard line: a Message inside a
 * Thread you are not part of is not reportable, because private is private by
 * construction. The queue asks {@link #forModeration}, which does not, because the
 * owner cannot judge a Report about something they are not allowed to look at.
 */
public interface ReportableContent {

    /**
     * The item as the reporter may see it, or empty when they may not — which the
     * report route turns into a 404, no placeholder, exactly as the Profile page does.
     */
    Optional<ReportedItem> visibleToReporter(TargetKind kind, long id, Optional<Member> viewer);

    /** The item as the queue must see it, visibility rules set aside. */
    Optional<ReportedItem> forModeration(TargetKind kind, long id);

    /**
     * Remove the item (§10.3 rung 1) as a dated fact, in {@code reply.removed_at}'s
     * shape. Idempotent; false means this contributor does not own the kind.
     *
     * <p>Removal is total and disclosed. There is deliberately no "reduce" or "limit"
     * here for a contributor to implement, because §10.3 refuses covert reach reduction
     * outright and an interface that could express it would be the first step back.
     */
    boolean remove(TargetKind kind, long id, Instant now);

    /**
     * Put a removed item back exactly as it was. Idempotent; false means this
     * contributor does not own the kind.
     *
     * <p>This exists for §10.5's auto-hide, which is a <em>reversible</em>, pre-decision
     * hold rather than a finding — and which therefore needs the item to stop rendering
     * everywhere, immediately, and to be able to come back untouched. Rather than give
     * a hold its own parallel set of read-path filters (which every module would have to
     * remember, and one would eventually forget), a hold uses the same dated fact
     * removal does. The difference between "removed" and "held" is not something the
     * modules need to know: it lives in moderation's own records, and in what the two
     * parties are told.
     */
    boolean restore(TargetKind kind, long id);

    /**
     * What the queue and the statement of reasons need to say about one item.
     *
     * @param authorId the Member answerable for it, where there is one — a Company page
     *                 is written by many hands and named by none, so it has no author,
     *                 and the ladder's member-facing rungs simply do not apply to it.
     * @param summary  the item in the owner's own view, enough to judge it by.
     * @param href     where it lives, so the decision is made against the real thing.
     */
    record ReportedItem(TargetKind kind,
                        long id,
                        Optional<Long> authorId,
                        String summary,
                        String href,
                        boolean removed) {
    }
}
