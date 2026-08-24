package com.mbeebe.docket.invites;

import com.mbeebe.docket.graph.ConnectionRequests;
import com.mbeebe.docket.identity.JoinListener;
import com.mbeebe.docket.identity.Member;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Where an Invite lands: the moment a Member first exists with an invited address
 * (§13.3), every unspent Invite to it becomes the Connection request that is
 * already waiting.
 *
 * <p>Keyed on the address and on nothing else — no token, no invited-signup route.
 * That is what makes §13.3's "optional, never a gate" structural: the invitee can
 * ignore the mail entirely, join from the front page months later, and still find
 * the request; and an invited signup is byte-for-byte an ordinary one (§3.1) up
 * to the instant this runs.
 *
 * <p>It also means the age gate is survived rather than special-cased. A refused
 * under-16 never becomes a Member (§3.1 stores nothing about the refusal), so
 * nothing here fires and the Invite simply stays unspent — no row anywhere
 * records that a refusal happened.
 *
 * <p>Several people may have invited the same address. All of them land, in the
 * order they were sent; each is a separate Connection request, exactly as it
 * would be if all of them had pressed Connect a second after the person joined.
 */
@Component
class InviteLanding implements JoinListener {

    private final InviteRepository invites;
    private final ConnectionRequests requests;
    private final Clock clock;

    InviteLanding(InviteRepository invites, ConnectionRequests requests, Clock clock) {
        this.invites = invites;
        this.requests = requests;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void joined(Member member) {
        for (Invite invite : invites.findByEmailIgnoreCaseAndLandedAtIsNull(member.email())) {
            // Spent either way. Whether the request materialised is the graph's
            // business and is deliberately neither returned nor recorded: §9.2 can
            // refuse an adult's request to this 16-year-old, and the adult must not
            // be able to find that out (see graph.ConnectionRequests).
            invite.land(clock.instant());
            requests.waitingFrom(invite.senderId(), member.id(), invite.note());
        }
    }
}
