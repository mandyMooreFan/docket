package com.mbeebe.docket.graph;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * The graph's workflows (§4.2, §4.3, §7.3). The outcomes are deliberately coarse:
 *
 * <ul>
 *   <li>{@code NOT_THERE} renders as the same 404 a nonexistent member gets — a
 *       Block never answers differently from nothing existing (§7.3);</li>
 *   <li>{@code REFUSED} is the loud server-side no: §9.2's adult-to-minor rule,
 *       the §3.2 capability bar, a non-Connection recommending;</li>
 *   <li>{@code DONE} covers the silent swallows too, because a declined or
 *       duplicate request must look exactly like a fresh one (§4.2).</li>
 * </ul>
 */
@Service
class GraphService {

    enum Outcome { DONE, NOT_THERE, REFUSED }

    private final ConnectionRequestRepository requests;
    private final ConnectionRepository connectionRows;
    private final MemberBlockRepository blocks;
    private final RecommendationRepository recommendations;
    private final Connections graph;
    private final Members members;
    private final CapabilityService capabilities;
    private final ProfileService profiles;
    private final Clock clock;

    GraphService(ConnectionRequestRepository requests, ConnectionRepository connectionRows,
                 MemberBlockRepository blocks, RecommendationRepository recommendations,
                 Connections graph, Members members, CapabilityService capabilities,
                 ProfileService profiles, Clock clock) {
        this.requests = requests;
        this.connectionRows = connectionRows;
        this.blocks = blocks;
        this.recommendations = recommendations;
        this.graph = graph;
        this.members = members;
        this.capabilities = capabilities;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Transactional
    Outcome request(Member sender, long recipientId, String rawNote) {
        Optional<Member> recipient = members.find(recipientId);
        if (recipient.isEmpty() || graph.blocked(sender.id(), recipientId)) {
            // §7.3: across a Block, the other member does not exist — either way round.
            return Outcome.NOT_THERE;
        }
        if (recipientId == sender.id()) {
            return Outcome.DONE;
        }
        // §9.2: an adult cannot send a request to an under-18; the reverse works.
        if (!sender.isMinor() && recipient.get().isMinor()) {
            return Outcome.REFUSED;
        }
        // §3.2: connecting is a Capability, earned by Completeness.
        if (capabilities.may(sender.id(), Capability.CONNECT) != CapabilityAnswer.YES) {
            return Outcome.REFUSED;
        }
        if (graph.connected(sender.id(), recipientId)
                || pendingBetween(sender.id(), recipientId)
                || requests.existsByRequesterIdAndRecipientIdAndState(
                        sender.id(), recipientId, ConnectionRequest.State.DECLINED)) {
            // Already connected, already asked, asked by them, or declined once
            // (§4.2 blocks repeats): all swallowed with the same outward answer a
            // fresh send gets, so a decline can never be observed from outside.
            return Outcome.DONE;
        }
        requests.save(new ConnectionRequest(sender.id(), recipientId,
                rawNote == null ? "" : rawNote.strip(), clock.instant()));
        return Outcome.DONE;
    }

    private boolean pendingBetween(long one, long other) {
        return requests.existsByRequesterIdAndRecipientIdAndState(
                        one, other, ConnectionRequest.State.PENDING)
                || requests.existsByRequesterIdAndRecipientIdAndState(
                        other, one, ConnectionRequest.State.PENDING);
    }

    /** True when there was a pending request from this requester to act on. */
    @Transactional
    boolean accept(Member recipient, long requesterId) {
        if (graph.blocked(recipient.id(), requesterId)) {
            return false;
        }
        return requests.findByRequesterIdAndRecipientIdAndState(
                        requesterId, recipient.id(), ConnectionRequest.State.PENDING)
                .map(request -> {
                    request.accept(clock.instant());
                    if (!graph.connected(requesterId, recipient.id())) {
                        connectionRows.save(new Connection(requesterId, recipient.id(),
                                clock.instant()));
                    }
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    boolean decline(Member recipient, long requesterId) {
        if (graph.blocked(recipient.id(), requesterId)) {
            return false;
        }
        return requests.findByRequesterIdAndRecipientIdAndState(
                        requesterId, recipient.id(), ConnectionRequest.State.PENDING)
                .map(request -> {
                    request.decline(clock.instant());
                    return true;
                })
                .orElse(false);
    }

    /** Disconnect (§4.2, §7.3): quiet, reversible, idempotent — the row simply ends. */
    @Transactional
    void disconnect(Member member, long otherId) {
        connectionRows.findByMemberAAndMemberB(Math.min(member.id(), otherId),
                        Math.max(member.id(), otherId))
                .ifPresent(connectionRows::delete);
    }

    /** Block (§7.3): total and durable. Severs the Connection in the same stroke. */
    @Transactional
    Outcome block(Member blocker, long otherId) {
        if (otherId == blocker.id() || members.find(otherId).isEmpty()
                || graph.blocked(blocker.id(), otherId)) {
            return Outcome.NOT_THERE;
        }
        blocks.save(new MemberBlock(blocker.id(), otherId, clock.instant()));
        connectionRows.findByMemberAAndMemberB(Math.min(blocker.id(), otherId),
                        Math.max(blocker.id(), otherId))
                .ifPresent(connectionRows::delete);
        return Outcome.DONE;
    }

    /** §4.3: only a Connection may write one; it waits, unseen, for the subject. */
    @Transactional
    Outcome recommend(Member author, long subjectId, String rawText) {
        if (members.find(subjectId).isEmpty() || graph.blocked(author.id(), subjectId)) {
            return Outcome.NOT_THERE;
        }
        if (subjectId == author.id() || !graph.connected(author.id(), subjectId)) {
            return Outcome.REFUSED;
        }
        String text = rawText == null ? "" : rawText.strip();
        if (text.isEmpty()) {
            return Outcome.DONE;
        }
        Optional<Recommendation> existing =
                recommendations.findByAuthorIdAndSubjectId(author.id(), subjectId);
        if (existing.isPresent()) {
            // Unapproved words may still be rewritten; once the subject has ruled
            // (approved or hidden), a rewrite would dodge that ruling — quietly no.
            if (existing.get().awaitingApproval()) {
                existing.get().rewrite(text, clock.instant());
            }
            return Outcome.DONE;
        }
        recommendations.save(new Recommendation(author.id(), subjectId, text, clock.instant()));
        return Outcome.DONE;
    }

    /** True when a recommendation from this author was there for the subject to approve. */
    @Transactional
    boolean approve(Member subject, long authorId) {
        return recommendations.findByAuthorIdAndSubjectId(authorId, subject.id())
                .filter(Recommendation::awaitingApproval)
                .map(recommendation -> {
                    recommendation.approve(clock.instant());
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    boolean hide(Member subject, long authorId) {
        return recommendations.findByAuthorIdAndSubjectId(authorId, subject.id())
                .filter(recommendation -> !recommendation.hidden())
                .map(recommendation -> {
                    recommendation.hide(clock.instant());
                    return true;
                })
                .orElse(false);
    }

    /** The Network page: pending incoming requests, then the member's own list (§13.4). */
    @Transactional(readOnly = true)
    NetworkPage networkFor(long memberId) {
        var pending = requests.findByRecipientIdAndStateOrderBySentAt(
                        memberId, ConnectionRequest.State.PENDING).stream()
                .filter(request -> !graph.blocked(memberId, request.requesterId()))
                .map(request -> new NetworkPage.RequestCard(
                        profiles.cardFor(request.requesterId()), request.note()))
                .toList();
        var connections = graph.connectedTo(memberId).stream()
                .map(profiles::cardFor)
                .toList();
        return new NetworkPage(pending, connections);
    }
}
