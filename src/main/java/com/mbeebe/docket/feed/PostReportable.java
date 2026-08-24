package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.moderation.ReportableContent;
import com.mbeebe.docket.moderation.TargetKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The feed module's answer to {@link ReportableContent} (§10.2, §10.3): the two
 * kinds this module owns the rows for, POST and REPLY. Every other kind is
 * {@link Optional#empty()} — not a refusal, just "not mine", so the registry
 * falls through to the module that does own it.
 *
 * <p>{@link #visibleToReporter} asks {@link PostService#visibleTo} and nothing
 * else, which is the whole point of the seam: a reporter may report exactly what
 * they could already read — the author's Dial with §4.1's floors, §7.3's Blocks,
 * §9.4's permanent cap, and §10.3's own removal. A Reply rides its Post's
 * visibility for the same reason the Post's page does: the thread is where a
 * Reply is read, so a Reply nobody can reach is a Reply nobody can report.
 *
 * <p>{@link #forModeration} sets that aside — the queue cannot judge a Report
 * about something it is not allowed to look at — but still answers empty for a
 * row that does not exist, and reports the removal that has already happened so
 * the ladder never claims to remove the same item twice.
 */
@Component
class PostReportable implements ReportableContent {

    private final PostRepository posts;
    private final ReplyRepository replies;
    private final PostService postService;

    PostReportable(PostRepository posts, ReplyRepository replies, PostService postService) {
        this.posts = posts;
        this.replies = replies;
        this.postService = postService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> visibleToReporter(TargetKind kind, long id,
                                                    Optional<Member> viewer) {
        return switch (kind) {
            case POST -> posts.findById(id)
                    .filter(post -> postService.visibleTo(post, viewer))
                    .map(PostReportable::postItem);
            case REPLY -> replies.findById(id)
                    .filter(reply -> !reply.removed())
                    .filter(reply -> posts.findById(reply.postId())
                            .map(post -> postService.visibleTo(post, viewer))
                            .orElse(false))
                    .map(PostReportable::replyItem);
            default -> Optional.empty();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> forModeration(TargetKind kind, long id) {
        return switch (kind) {
            case POST -> posts.findById(id).map(PostReportable::postItem);
            case REPLY -> replies.findById(id).map(PostReportable::replyItem);
            default -> Optional.empty();
        };
    }

    @Override
    @Transactional
    public boolean remove(TargetKind kind, long id, Instant now) {
        switch (kind) {
            case POST -> posts.findById(id).ifPresent(post -> post.remove(now));
            case REPLY -> replies.findById(id).ifPresent(reply -> reply.remove(now));
            default -> {
                return false;
            }
        }
        // True means "this contributor owns the kind", not "a row changed":
        // removal is idempotent and a missing row is already not rendering.
        return true;
    }

    @Override
    @Transactional
    public boolean restore(TargetKind kind, long id) {
        switch (kind) {
            case POST -> posts.findById(id).ifPresent(Post::restore);
            case REPLY -> replies.findById(id).ifPresent(Reply::restore);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static ReportedItem postItem(Post post) {
        return new ReportedItem(TargetKind.POST, post.id(), Optional.of(post.authorId()),
                post.body(), "/posts/" + post.id(), post.removed());
    }

    /**
     * A Reply's href is its thread's, because a Reply has no page of its own —
     * §5.3's shape, and the decision is better made against the thread anyway.
     */
    private static ReportedItem replyItem(Reply reply) {
        return new ReportedItem(TargetKind.REPLY, reply.id(), Optional.of(reply.authorId()),
                reply.body(), "/posts/" + reply.postId(), reply.removed());
    }
}
