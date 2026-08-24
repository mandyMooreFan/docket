package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.Images;
import com.mbeebe.docket.profile.ConnectionLookup;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Posts and their threads (§5.2–§5.4). Visibility is derived at read time
 * (ADR-0002): a Post rides its author's Profile Dial with the same floors —
 * and §9.4's cap on top: content authored as a minor is members-only
 * regardless of everything else, permanently. The reply count is derived per
 * viewer and counts exactly what that viewer's rendering shows.
 */
@Service
class PostService {

    static final int MAX_BODY = 40_000;
    static final int MAX_REPLY = 2_000;
    static final int MAX_IMAGES = 4;

    /** A refusal with an honest, member-facing reason; rolls the write back. */
    static class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    /** GraphService's coarse shape: NOT_THERE renders as a 404, REFUSED as a 403. */
    enum Outcome { DONE, NOT_THERE, REFUSED }

    private final PostRepository posts;
    private final PostImageRepository postImages;
    private final ReplyRepository replies;
    private final SavedPostRepository saves;
    private final Images images;
    private final ProfileService profiles;
    private final ConnectionLookup graph;
    private final JobBoardLookup board;
    private final Clock clock;

    PostService(PostRepository posts, PostImageRepository postImages, ReplyRepository replies,
                SavedPostRepository saves, Images images, ProfileService profiles,
                ConnectionLookup graph, JobBoardLookup board, Clock clock) {
        this.posts = posts;
        this.postImages = postImages;
        this.replies = replies;
        this.saves = saves;
        this.images = images;
        this.profiles = profiles;
        this.graph = graph;
        this.board = board;
        this.clock = clock;
    }

    /**
     * A written Post (§5.2.1). Images go through the one §10.4 store; any
     * refusal refuses the whole Post — nothing partial ever lands.
     */
    @Transactional
    long compose(Member author, String rawBody, List<MultipartFile> files) {
        String body = rawBody == null ? "" : rawBody.strip();
        if (body.isEmpty()) {
            throw new Refused("A post needs words.");
        }
        if (body.length() > MAX_BODY) {
            throw new Refused("A post can hold at most 40,000 characters.");
        }
        // §9.4: the authored-as-minor fact, fixed here and never after.
        Post post = posts.save(new Post(author.id(), Post.Kind.WRITTEN, body,
                author.isMinor(), clock.instant()));
        int position = 0;
        for (MultipartFile file : files == null ? List.<MultipartFile>of() : files) {
            if (file.isEmpty()) {
                continue;
            }
            if (position == MAX_IMAGES) {
                throw new Refused("A post can carry at most four images.");
            }
            storeImage(post.id(), file, position++);
        }
        return post.id();
    }

    private void storeImage(long postId, MultipartFile file, int position) {
        Images.Stored stored;
        try {
            stored = images.store(file.getBytes(), file.getContentType());
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
        switch (stored.outcome()) {
            case STORED -> postImages.save(new PostImage(postId, stored.imageId(), position));
            case TOO_LARGE -> throw new Refused("Images are capped at 512KB.");
            case WRONG_TYPE -> throw new Refused("Images are PNG or JPEG.");
            case REFUSED -> throw new Refused("An image was refused by the upload checks.");
        }
    }

    /** The Post's page as this viewer may see it; empty — a plain 404 — when they may not. */
    @Transactional(readOnly = true)
    Optional<PostPage> pageFor(long postId, Optional<Member> viewer) {
        return posts.findById(postId)
                .filter(post -> visibleTo(post, viewer))
                .map(post -> {
                    List<ReplyView> replyViews = visibleReplies(post, viewer).stream()
                            .map(reply -> new ReplyView(reply.id(),
                                    profiles.cardFor(reply.authorId()),
                                    when(reply.createdAt()),
                                    PostBodies.toHtml(reply.body())))
                            .toList();
                    boolean isAuthor = owns(viewer, post);
                    boolean mayReply = mayReply(post, viewer);
                    return new PostPage(view(post, viewer), replyViews, mayReply,
                            post.threadClosed(), isAuthor,
                            viewer.isPresent() && !isAuthor && !mayReply && !post.threadClosed(),
                            viewer.isPresent());
                });
    }

    /**
     * §5.3: a Reply comes from one of the Post author's Connections — even on
     * a Post a stranger can read — into a thread that is still open. The
     * author may answer in their own thread. A Reply is not a Post
     * (CONTEXT.md), so §3.2's POST capability is deliberately not asked for:
     * the Connection itself was the earned thing.
     */
    @Transactional
    Outcome reply(Member member, long postId, String rawBody) {
        Optional<Post> post = posts.findById(postId)
                .filter(found -> visibleTo(found, Optional.of(member)));
        if (post.isEmpty()) {
            return Outcome.NOT_THERE;
        }
        if (!mayReply(post.get(), Optional.of(member))) {
            return Outcome.REFUSED;
        }
        String body = rawBody == null ? "" : rawBody.strip();
        if (body.isEmpty()) {
            throw new Refused("A reply needs words.");
        }
        if (body.length() > MAX_REPLY) {
            throw new Refused("A reply can hold at most 2,000 characters.");
        }
        // §9.4: the authored-as-minor fact, fixed here and never after.
        replies.save(new Reply(postId, member.id(), body, member.isMinor(), clock.instant()));
        return Outcome.DONE;
    }

    /** §5.3: the author may remove any Reply from their thread. True when there was one. */
    @Transactional
    boolean removeReply(Member member, long postId, long replyId) {
        if (!authoredBy(member, postId)) {
            return false;
        }
        return replies.findById(replyId)
                .filter(reply -> reply.postId() == postId)
                .map(reply -> {
                    reply.remove(clock.instant());
                    return true;
                })
                .orElse(false);
    }

    /** §5.3: the author may close the thread — no further Replies. */
    @Transactional
    boolean closeThread(Member member, long postId) {
        if (!authoredBy(member, postId)) {
            return false;
        }
        posts.findById(postId).orElseThrow().closeThread(clock.instant());
        return true;
    }

    /**
     * §5.4: a Post rides the author's single Dial — no per-Post visibility.
     * The Profile page's own derivation (Dial, floors, Blocks) is reused
     * wholesale: a Post is visible exactly when its author's page is. On top,
     * §9.4: a Post authored as a minor is never visible logged-out, with no
     * placeholder — and the 18 rollover never lifts that.
     *
     * <p>§10.3 rung 1 is asked first and of everyone, the author included: a
     * removed Post is visible to nobody. Putting it here rather than at each
     * surface is what makes the Post page, /saved, search and {@link
     * PostImageAudience} agree without any of them restating the rule.
     */
    boolean visibleTo(Post post, Optional<Member> viewer) {
        if (post.removed()) {
            return false;
        }
        if (viewer.isEmpty() && post.authoredAsMinor()) {
            return false;
        }
        return profiles.pageFor(post.authorId(), viewer).isPresent();
    }

    /** The Post as one viewer sees it, reply count included — theirs, nobody's else. */
    PostView view(Post post, Optional<Member> viewer) {
        // §5.2.2: the attached posting renders as a compact card; a reference
        // the board can no longer answer simply renders no card.
        JobBoardLookup.AttachedPosting attached = post.jobPostingId() == null ? null
                : board.attached(post.jobPostingId()).orElse(null);
        return new PostView(post.id(), profiles.cardFor(post.authorId()),
                when(post.createdAt()), PostBodies.toHtml(post.body()),
                postImages.findByPostIdOrderByPosition(post.id()).stream()
                        .map(PostImage::imageId).toList(),
                PostBodies.previews(post.body()),
                visibleReplies(post, viewer).size(),
                viewer.map(member ->
                        saves.existsByMemberIdAndPostId(member.id(), post.id())).orElse(false),
                owns(viewer, post), attached);
    }

    /** §5.3: the private Save. Idempotent; only a Post you can see can be kept. */
    @Transactional
    boolean save(Member member, long postId) {
        Optional<Post> post = posts.findById(postId)
                .filter(found -> visibleTo(found, Optional.of(member)));
        if (post.isEmpty()) {
            return false;
        }
        if (saves.findByMemberIdAndPostId(member.id(), postId).isEmpty()) {
            saves.save(new SavedPost(member.id(), postId, clock.instant()));
        }
        return true;
    }

    @Transactional
    boolean unsave(Member member, long postId) {
        saves.findByMemberIdAndPostId(member.id(), postId).ifPresent(saves::delete);
        return true;
    }

    /**
     * The member's /saved page — theirs alone. A saved Post that has since
     * left their audience (a Dial turned down, a Block) simply doesn't render;
     * the Save itself stays private either way.
     */
    @Transactional(readOnly = true)
    List<PostView> savedFor(Member member) {
        return saves.findByMemberIdOrderBySavedAtDescIdDesc(member.id()).stream()
                .flatMap(saved -> posts.findById(saved.postId()).stream())
                .filter(post -> visibleTo(post, Optional.of(member)))
                .map(post -> view(post, Optional.of(member)))
                .toList();
    }

    /**
     * §5.3 + §9.4: the Replies one viewer's rendering shows. Removed Replies
     * are gone for everyone; a Block hides a Reply from the blocked viewer; a
     * logged-out view omits minor-authored Replies with no placeholder. The
     * count is this list's size — it can never disagree with the page.
     */
    private List<Reply> visibleReplies(Post post, Optional<Member> viewer) {
        return replies.findByPostIdAndRemovedAtIsNullOrderByCreatedAtAscIdAsc(post.id()).stream()
                .filter(reply -> viewer
                        .map(member -> member.id() == reply.authorId()
                                || !graph.blocked(member.id(), reply.authorId()))
                        .orElse(!reply.authoredAsMinor()))
                .toList();
    }

    private boolean mayReply(Post post, Optional<Member> viewer) {
        if (viewer.isEmpty() || post.threadClosed()) {
            return false;
        }
        long viewerId = viewer.get().id();
        return viewerId == post.authorId() || graph.connected(viewerId, post.authorId());
    }

    private boolean authoredBy(Member member, long postId) {
        return posts.findById(postId)
                .map(post -> post.authorId() == member.id())
                .orElse(false);
    }

    private static boolean owns(Optional<Member> viewer, Post post) {
        return viewer.map(member -> member.id() == post.authorId()).orElse(false);
    }

    private String when(java.time.Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone()).format(instant);
    }
}
