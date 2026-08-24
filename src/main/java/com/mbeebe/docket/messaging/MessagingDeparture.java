package com.mbeebe.docket.messaging;

import com.mbeebe.docket.leaving.Departure;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Messaging at Termination (§11.2) — the contributor that mostly exists to say
 * what it does <em>not</em> do.
 *
 * <p><strong>Threads, Messages and their images stay, every one of them.</strong>
 * This is §11.2's named accepted cost: "your side of a Thread stays, attributed to
 * a former member … you cannot fully disappear from Docket — the alternative is
 * 'delete my account' deleting a colleague's correspondence into a monologue of
 * holes". V8 built the mechanism before this ticket existed: {@code
 * thread.member_a}, {@code thread.member_b} and {@code message.author_id}
 * deliberately do not cascade, so the rows cannot be taken out from under the
 * other party even by accident. Nothing in this class adds a cascade, and nothing
 * ever should.
 *
 * <p>The attribution arrives without a write. {@code PersonCard} asks identity
 * whether a Member is terminated on every render, so the other party's Thread
 * shows "A former member" from the next request onward (ADR-0002 again: the
 * conclusion is derived, so there is no denormalised name to go and rewrite in
 * every Message). The Thread also stops being writable on its own — {@code
 * channelOpen} needs a Connection or an open Application, and {@code
 * GraphDeparture} and {@code JobsDeparture} have just removed both — so it settles
 * into the same read-only history a Disconnect leaves, with the same sentence.
 *
 * <p>What does go is the leaver's own read marks. They are one-sided by
 * construction (§7.2: no read receipts, ever), so nobody can observe their
 * absence, and an unread count for an account that no longer exists is nothing at
 * all.
 *
 * <p>The genuinely unresolved part, recorded honestly (§11.2, {@code
 * docs/data-rights.md} §3): no authority squarely addresses erasing a two-party
 * Thread on account closure. Docket's position — the history is still necessary
 * for the other party's access to their own correspondence, so Art. 17(1)(a) never
 * fires — is an argument to defend rather than a settled one, and the fallback if
 * it fails is already half-built here: de-identify the attribution, keep the text,
 * which is exactly what this class does. LAWYER at the §15.6 gate.
 */
@Component
@Order(70)
class MessagingDeparture implements Departure {

    private final ThreadReadRepository marks;

    MessagingDeparture(ThreadReadRepository marks) {
        this.marks = marks;
    }

    @Override
    @Transactional
    public void memberLeaving(long memberId) {
        marks.deleteByMemberId(memberId);
    }
}
