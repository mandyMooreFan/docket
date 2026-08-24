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

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The feed (§5.1): distribution is the mutual graph and nothing else,
 * reverse-chronological, and it ends. The read position is a single high-water
 * mark advanced past whatever a view rendered — Posts and reply notices alike —
 * so nothing is ever shown twice and nothing re-surfaces. The accepted cost,
 * argued in #33's PR: a refresh lands on "You're caught up", and older Posts
 * are reached from the author's Profile — which is exactly where §5.1 sends
 * them.
 *
 * <p>§5.5 against §5.6: replies to you surface as a quiet section at the top
 * of this page and nowhere else — no badge, no dot, no number, no email. The
 * feed never comes to get you.
 */
@Service
class FeedService {

    private final PostRepository posts;
    private final ReplyRepository replies;
    private final PostService postService;
    private final FeedVisitRepository visits;
    private final Connections graph;
    private final CapabilityService capabilities;
    private final ProfileService profiles;
    private final Clock clock;

    FeedService(PostRepository posts, ReplyRepository replies, PostService postService,
                FeedVisitRepository visits, Connections graph, CapabilityService capabilities,
                ProfileService profiles, Clock clock) {
        this.posts = posts;
        this.replies = replies;
        this.postService = postService;
        this.visits = visits;
        this.graph = graph;
        this.capabilities = capabilities;
        this.profiles = profiles;
        this.clock = clock;
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
        Instant mark = visits.findById(member.id())
                .map(FeedVisit::seenUpTo)
                .orElse(Instant.EPOCH);

        List<Reply> freshReplies = repliesToYou(member, mark);
        List<FeedPage.ReplyNotice> notices = freshReplies.stream()
                .map(reply -> new FeedPage.ReplyNotice(reply.postId(),
                        profiles.cardFor(reply.authorId()), when(reply.createdAt()),
                        PostBodies.excerpt(reply.body())))
                .toList();

        // §5.1, §13.4: the mutual graph and nothing else — empty means empty.
        List<Long> connections = graph.connectedTo(member.id());
        List<Post> freshPosts = connections.isEmpty()
                ? List.of()
                : posts.findByAuthorIdInAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                        connections, mark);
        List<PostView> entries = freshPosts.stream()
                .map(post -> postService.view(post, Optional.of(member)))
                .toList();

        if (advance) {
            latestOf(freshPosts, freshReplies)
                    .ifPresent(seenUpTo -> advanceMark(member.id(), seenUpTo));
        }
        return new FeedPage(!connections.isEmpty(), mayPost, notices, entries, pending);
    }

    /**
     * §5.5, the entire list: unremoved replies newer than the mark, in your own
     * threads or threads you joined with a still-standing Reply, written by
     * someone other than you whom no Block separates you from — and only where
     * the thread's Post is still yours to see.
     */
    private List<Reply> repliesToYou(Member member, Instant mark) {
        Set<Long> threadIds = new LinkedHashSet<>();
        posts.findByAuthorIdOrderByCreatedAtDescIdDesc(member.id())
                .forEach(post -> threadIds.add(post.id()));
        threadIds.addAll(replies.postIdsJoinedBy(member.id()));
        if (threadIds.isEmpty()) {
            return List.of();
        }
        return replies
                .findByPostIdInAndRemovedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                        threadIds, mark)
                .stream()
                .filter(reply -> reply.authorId() != member.id())
                .filter(reply -> !graph.blocked(member.id(), reply.authorId()))
                .filter(reply -> posts.findById(reply.postId())
                        .map(post -> postService.visibleTo(post, Optional.of(member)))
                        .orElse(false))
                .toList();
    }

    private static Optional<Instant> latestOf(List<Post> freshPosts, List<Reply> freshReplies) {
        Instant fromPosts = freshPosts.isEmpty() ? null : freshPosts.get(0).createdAt();
        Instant fromReplies = freshReplies.isEmpty() ? null : freshReplies.get(0).createdAt();
        if (fromPosts == null) {
            return Optional.ofNullable(fromReplies);
        }
        if (fromReplies == null) {
            return Optional.of(fromPosts);
        }
        return Optional.of(fromPosts.isAfter(fromReplies) ? fromPosts : fromReplies);
    }

    private void advanceMark(long memberId, Instant seenUpTo) {
        visits.findById(memberId).ifPresentOrElse(
                visit -> visit.advanceTo(seenUpTo),
                () -> visits.save(new FeedVisit(memberId, seenUpTo)));
    }

    private String when(Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone()).format(instant);
    }
}
