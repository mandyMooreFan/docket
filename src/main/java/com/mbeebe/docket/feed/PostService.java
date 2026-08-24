package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.Images;
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
 * Posts themselves (§5.2, §5.4): writing one, and rendering one to the viewer
 * it is allowed to reach. Visibility is derived at read time (ADR-0002): a Post
 * rides its author's Profile Dial with the same floors — and §9.4's cap on top:
 * a Post authored as a minor is members-only regardless of everything else,
 * permanently.
 */
@Service
class PostService {

    static final int MAX_BODY = 40_000;
    static final int MAX_IMAGES = 4;

    /** A refusal with an honest, member-facing reason; rolls the write back. */
    static class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    private final PostRepository posts;
    private final PostImageRepository postImages;
    private final Images images;
    private final ProfileService profiles;
    private final Clock clock;

    PostService(PostRepository posts, PostImageRepository postImages, Images images,
                ProfileService profiles, Clock clock) {
        this.posts = posts;
        this.postImages = postImages;
        this.images = images;
        this.profiles = profiles;
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

    /** The Post as this viewer may see it; empty — a plain 404 — when they may not. */
    @Transactional(readOnly = true)
    Optional<PostView> pageFor(long postId, Optional<Member> viewer) {
        return posts.findById(postId)
                .filter(post -> visibleTo(post, viewer))
                .map(post -> view(post, viewer));
    }

    /**
     * §5.4: a Post rides the author's single Dial — no per-Post visibility. The
     * Profile page's own derivation (Dial, floors, Blocks) is reused wholesale:
     * a Post is visible exactly when its author's page is. On top, §9.4: a Post
     * authored as a minor is never visible logged-out, with no placeholder.
     */
    boolean visibleTo(Post post, Optional<Member> viewer) {
        if (viewer.isEmpty() && post.authoredAsMinor()) {
            return false;
        }
        return profiles.pageFor(post.authorId(), viewer).isPresent();
    }

    PostView view(Post post, Optional<Member> viewer) {
        return new PostView(post.id(), profiles.cardFor(post.authorId()),
                when(post), PostBodies.toHtml(post.body()),
                postImages.findByPostIdOrderByPosition(post.id()).stream()
                        .map(PostImage::imageId).toList(),
                PostBodies.previews(post.body()),
                0, false,
                viewer.map(member -> member.id() == post.authorId()).orElse(false));
    }

    private String when(Post post) {
        return DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone()).format(post.createdAt());
    }
}
