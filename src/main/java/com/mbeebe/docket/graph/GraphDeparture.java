package com.mbeebe.docket.graph;

import com.mbeebe.docket.leaving.Departure;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The graph at Termination (§11.2) — and the one place in this ticket where two
 * halves of the same module go opposite ways, for a reason worth stating.
 *
 * <p><strong>Connections, requests and Blocks go.</strong> All three are facts
 * about a live relationship between two live people, and one of the two has
 * stopped existing. A Connection to a former member is not a Connection; a pending
 * request is waiting on an answer that will never come; a Block is protection from
 * somebody who can no longer reach anyone. Leaving them would also leave the other
 * party a count that includes a ghost, which is the sort of quiet inaccuracy §4.2
 * spends a whole section refusing.
 *
 * <p><strong>Recommendations the leaver wrote stay published, untouched.</strong>
 * §11.2 says so outright, and the research says why: the evaluative content is
 * primarily the <em>subject's</em> personal data, not the author's, and what is the
 * author's in it is chiefly the attribution ({@code docs/data-rights.md} §3).
 * De-identifying the attribution while keeping the text is exactly EDPB Guidelines
 * 01/2022 ¶173's "reconcile rather than refuse", and it is what happens here for
 * free: the row is not touched, the Profile behind it is gone, and
 * {@code PersonCard} renders "A former member". Nobody's page loses a
 * recommendation because the person who wrote it closed their account.
 *
 * <p><strong>Recommendations written about the leaver also stay</strong>, and stop
 * being reachable by anyone — they render only on the subject's Profile, and that
 * page now 404s. They are somebody else's words, and this ticket is deliberately
 * not in the business of deleting those. Recorded as a decision, not a leftover.
 * Whether that is the right answer under Article 17 is genuinely open
 * ({@code data-rights.md} §8 item 2); §11.2's LAWYER gate is where it gets settled.
 */
@Component
@Order(30)
class GraphDeparture implements Departure {

    private final ConnectionRepository connections;
    private final ConnectionRequestRepository requests;
    private final MemberBlockRepository blocks;

    GraphDeparture(ConnectionRepository connections, ConnectionRequestRepository requests,
                   MemberBlockRepository blocks) {
        this.connections = connections;
        this.requests = requests;
        this.blocks = blocks;
    }

    @Override
    @Transactional
    public void memberLeaving(long memberId) {
        connections.findByMemberAOrMemberBOrderByConnectedAtDesc(memberId, memberId)
                .forEach(connections::delete);
        requests.findByRequesterId(memberId).forEach(requests::delete);
        requests.findByRecipientId(memberId).forEach(requests::delete);
        blocks.findByBlockerId(memberId).forEach(blocks::delete);
        blocks.findByBlockedId(memberId).forEach(blocks::delete);
    }
}
