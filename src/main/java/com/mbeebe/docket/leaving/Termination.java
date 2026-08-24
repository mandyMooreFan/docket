package com.mbeebe.docket.leaving;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The end of a Member (CONTEXT.md; SPEC.md §11.2), in one place and behind one
 * call.
 *
 * <p><strong>The seam #38 asked for.</strong> The §10.3 moderation ladder's fourth
 * rung is termination, and §11.2's is this ticket's; both need the same primitive
 * and there must only be one. This is it — {@code terminate(member, reason)}, with
 * {@link Reason} carrying which of the two doors it came through. Moderation calls
 * this rather than reimplementing the member-facing half, because the half that
 * matters is not the deletion: it is what survives it, and getting that wrong from
 * the ladder's side would delete a colleague's correspondence.
 *
 * <p>What this call does NOT do is offer the export. That is deliberate and it is
 * the one asymmetry between the two doors: §11.2 says deletion <em>offers the
 * export first and never requires it</em>, which is a thing a member is offered in
 * a page, not a thing a service does on their behalf — and a moderator terminating
 * an account is not going to be offered anything. {@code LeavingController} holds
 * the offer, above the confirmation, on the member's own path.
 *
 * <p>One transaction. Every {@link Departure} contributor takes its own rows out,
 * in order, and then identity writes the tombstone last (see {@code
 * Members#terminate}) — last because until it lands, the departing Member is still
 * findable, and several contributors need to find them.
 */
@Service
public class Termination {

    /** Which door this came through. Recorded, never derived from anything. */
    public enum Reason {

        /** §11.2: the member asked to leave. */
        MEMBER_REQUEST,

        /** §10.3's fourth rung: the ladder ended the account (#38). */
        MODERATION
    }

    // Resolved per call rather than injected: a contributor lives in the module
    // whose rows it deletes, and those modules do not depend on this one, so
    // eager injection would order the container against the dependency graph.
    // The same shape, and the same reason, as images.ImageAudiences.
    private final ObjectProvider<Departure> departures;
    private final Members members;

    Termination(ObjectProvider<Departure> departures, Members members) {
        this.departures = departures;
        this.members = members;
    }

    /**
     * Ends this Member. Idempotent: a Member already terminated is left exactly as
     * they are, so a double-submitted form and a ladder racing a member's own
     * departure both do nothing the second time.
     */
    @Transactional
    public void terminate(Member member, Reason reason) {
        if (member.terminated()) {
            return;
        }
        departures.orderedStream().forEach(departure -> departure.memberLeaving(member.id()));
        members.terminate(member.id(), reason.name());
    }
}
