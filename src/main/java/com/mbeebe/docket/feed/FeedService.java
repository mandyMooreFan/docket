package com.mbeebe.docket.feed;

import com.mbeebe.docket.graph.Connections;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The feed (§5.1): distribution is the mutual graph and nothing else,
 * reverse-chronological, and it ends. The per-member read position is a single
 * high-water mark advanced past whatever a view rendered — so nothing is ever
 * shown twice and nothing re-surfaces; older Posts are reached from the
 * author's Profile. The accepted cost: a refresh lands on "You're caught up".
 */
@Service
class FeedService {

    private final PostRepository posts;
    private final PostService postService;
    private final Connections graph;
    private final CapabilityService capabilities;

    FeedService(PostRepository posts, PostService postService, Connections graph,
                CapabilityService capabilities) {
        this.posts = posts;
        this.postService = postService;
        this.graph = graph;
        this.capabilities = capabilities;
    }

    /**
     * The feed as one member sees it. {@code advance} moves the read position
     * past what this call renders — true for a real visit, false when the page
     * is only being re-rendered around a composer error.
     */
    @Transactional
    FeedPage feedFor(Member member, boolean advance) {
        boolean mayPost =
                capabilities.may(member.id(), Capability.POST) == CapabilityAnswer.YES;
        List<Long> connections = graph.connectedTo(member.id());
        if (connections.isEmpty()) {
            // §13.4: the feed stays connections-only and is simply empty.
            return new FeedPage(false, mayPost, List.of(), List.of(), List.of());
        }
        List<PostView> entries = posts
                .findByAuthorIdInAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                        connections, Instant.EPOCH)
                .stream()
                .map(post -> postService.view(post, java.util.Optional.of(member)))
                .toList();
        return new FeedPage(true, mayPost, List.of(), entries, List.of());
    }
}
