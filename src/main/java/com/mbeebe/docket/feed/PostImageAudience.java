package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.ImageAudience;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The feed module's answer for /images/{id} (§5.4, §8.5, §9.4): an image on a Post is
 * visible exactly when the Post is.
 *
 * <p>Deliberately thin. It resolves the image to its Post and then defers to
 * {@link PostService#visibleTo}, the same derivation the Post's own page uses — the
 * author's Dial with §4.1's floors, §7.3's Blocks, and §9.4's permanent cap on
 * anything authored as a minor. Reimplementing any of that here would be a second
 * copy of the rules to keep in step, and the rule §8.5 states is that the Dial is
 * honoured on <em>every</em> surface, which only holds if every surface asks the same
 * question.
 *
 * <p>Never {@link Verdict#OPEN_WEB}, even for a Post that a logged-out visitor may
 * read today: that answer is derived from a Dial the author can turn down in the next
 * second (ADR-0002), and a shared cache would keep serving the bytes long after.
 */
@Component
class PostImageAudience implements ImageAudience {

    private final PostImageRepository postImages;
    private final PostRepository posts;
    private final PostService postService;

    PostImageAudience(PostImageRepository postImages, PostRepository posts,
                      PostService postService) {
        this.postImages = postImages;
        this.posts = posts;
        this.postService = postService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Verdict> verdictFor(long imageId, Optional<Member> viewer) {
        return postImages.findFirstByImageId(imageId)
                .flatMap(attachment -> posts.findById(attachment.postId()))
                .map(post -> postService.visibleTo(post, viewer)
                        ? Verdict.THIS_VIEWER
                        : Verdict.HIDDEN);
    }
}
