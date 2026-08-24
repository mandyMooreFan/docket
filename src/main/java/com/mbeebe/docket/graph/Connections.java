package com.mbeebe.docket.graph;

import com.mbeebe.docket.profile.ConnectionLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The graph's public face: the questions other modules may ask of Connections and
 * Blocks. Every answer — connected, blocked, the count, Mutuals — is derived from
 * the stored rows at the point of asking (ADR-0002); nothing here is cached or
 * flagged. Deliberately free of any profile dependency so visibility (which the
 * profile module derives through {@link ConnectionLookup}) can ask without a cycle.
 */
@Service
public class Connections implements ConnectionLookup {

    private final ConnectionRepository connections;
    private final MemberBlockRepository blocks;
    private final ConnectionRequestRepository requests;

    Connections(ConnectionRepository connections, MemberBlockRepository blocks,
                ConnectionRequestRepository requests) {
        this.connections = connections;
        this.blocks = blocks;
        this.requests = requests;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean connected(long memberA, long memberB) {
        return memberA != memberB && connections.existsByMemberAAndMemberB(
                Math.min(memberA, memberB), Math.max(memberA, memberB));
    }

    /** A Block in either direction (§7.3) — total and symmetric, so one question. */
    @Override
    @Transactional(readOnly = true)
    public boolean blocked(long memberA, long memberB) {
        return blocks.existsByBlockerIdAndBlockedId(memberA, memberB)
                || blocks.existsByBlockerIdAndBlockedId(memberB, memberA);
    }

    /** The §4.2 connection count — the only number the graph ever shows. */
    @Transactional(readOnly = true)
    public int countFor(long memberId) {
        return (int) connections.countByMemberAOrMemberB(memberId, memberId);
    }

    /** Everyone this Member is connected to, newest Connection first. */
    @Transactional(readOnly = true)
    public List<Long> connectedTo(long memberId) {
        return connections.findByMemberAOrMemberBOrderByConnectedAtDesc(memberId, memberId)
                .stream().map(connection -> connection.other(memberId)).toList();
    }

    /** Who is waiting on this Member's answer — the feed rail's real data (§2.3). */
    @Transactional(readOnly = true)
    public List<Long> pendingRequestersFor(long memberId) {
        return requests.findByRecipientIdAndStateOrderBySentAt(
                        memberId, ConnectionRequest.State.PENDING).stream()
                .map(ConnectionRequest::requesterId)
                .filter(requesterId -> !blocked(memberId, requesterId))
                .toList();
    }

    /** Mutuals (CONTEXT.md): the Connections two Members share — Docket names no other relationship. */
    @Transactional(readOnly = true)
    public List<Long> mutuals(long memberA, long memberB) {
        Set<Long> theirs = new HashSet<>(connectedTo(memberB));
        return connectedTo(memberA).stream().filter(theirs::contains).toList();
    }
}
