package com.mbeebe.docket.graph;

import com.mbeebe.docket.identity.Members;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one Connection request another module may ask the graph to make on a
 * member's behalf: the Invite's, at the moment the invitee joins (§13.3).
 *
 * <p>It goes through exactly the {@code GraphService.request} the profile page's
 * Connect button goes through, and that is the whole point — §13.3 says the
 * Invite <em>reuses</em> the Connection request rather than inventing a parallel
 * concept, so every rule that governs one governs the other with no second
 * implementation to keep in step: §9.2's adult-to-minor refusal, §3.2's
 * Completeness bar (re-derived here at the join rather than remembered from the
 * send, so a withdrawn capability composes in — #38), §7.3's Blocks, and §4.2's
 * silently swallowed duplicates and declines.
 *
 * <p>The outcome is deliberately discarded, and nothing is stored about it.
 * Nothing the inviter can observe distinguishes a request that materialised from
 * one that did not — which is what stops §9.2's refusal from telling an adult
 * that the person they invited turned out to be a child.
 */
@Service
public class ConnectionRequests {

    private final GraphService service;
    private final Members members;

    ConnectionRequests(GraphService service, Members members) {
        this.service = service;
        this.members = members;
    }

    /**
     * Leave a request from the inviter waiting for the new Member, if every rule
     * §4.2 and §9.2 already apply to a Connection request lets it exist.
     */
    @Transactional
    public void waitingFrom(long inviterId, long recipientId, String note) {
        members.find(inviterId)
                .ifPresent(inviter -> service.request(inviter, recipientId, note));
    }
}
