package com.mbeebe.docket.feed;

import com.mbeebe.docket.graph.Connections;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.PersonCard;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The feed (§5.1): distribution is the mutual graph and nothing else,
 * reverse-chronological, and it ends. The read position is a single high-water
 * mark advanced past whatever a view rendered — so nothing is ever shown twice
 * and nothing re-surfaces. The accepted cost, argued in #33's PR: a refresh
 * lands on "You're caught up", and older Posts are reached from the author's
 * Profile — which is exactly where §5.1 sends them.
 */
@Service
class FeedService {

    private final PostRepository posts;
    private final PostService postService;
    private final FeedVisitRepository visits;
    private final Connections graph;
    private final CapabilityService capabilities;
    private final ProfileService profiles;

    FeedService(PostRepository posts, PostService postService, FeedVisitRepository visits,
                Connections graph, CapabilityService capabilities, ProfileService profiles) {
        this.posts = posts;
        this.postService = postService;
        this.visits = visits;
        this.graph = graph;
        this.capabilities = capabilities;
        this.profiles = profiles;
    }

    /**
     * The feed as one member sees it. {@code advance} moves the read position
     * past what this call renders — true for a real visit, false when the page
     * is only re-rendered around a composer error.
     */
    @Transactional
    FeedPage feedFor(Member member, boolean advance) {
        boolean mayPost =
                capabilities.may(member.id(), Capability.POST) == CapabilityAnswer.YES;
        List<PersonCard> pending = graph.pendingRequestersFor(member.id()).stream()
                .map(profiles::cardFor)
                .toList();
        List<Long> connections = graph.connectedTo(member.id());
        if (connections.isEmpty()) {
            // §13.4: the feed stays connections-only and is simply empty — no fallback.
            return new FeedPage(false, mayPost, List.of(), List.of(), pending);
        }
        Instant mark = visits.findById(member.id())
                .map(FeedVisit::seenUpTo)
                .orElse(Instant.EPOCH);
        List<Post> fresh = posts.findByAuthorIdInAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                connections, mark);
        List<PostView> entries = fresh.stream()
                .map(post -> postService.view(post, Optional.of(member)))
                .toList();
        if (advance && !fresh.isEmpty()) {
            advanceMark(member.id(), fresh.get(0).createdAt());
        }
        return new FeedPage(true, mayPost, List.of(), entries, pending);
    }

    private void advanceMark(long memberId, Instant seenUpTo) {
        visits.findById(memberId).ifPresentOrElse(
                visit -> visit.advanceTo(seenUpTo),
                () -> visits.save(new FeedVisit(memberId, seenUpTo)));
    }
}
