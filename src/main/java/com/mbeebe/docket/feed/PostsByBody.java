package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.ProfileService;
import com.mbeebe.docket.search.PostSearch;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The feed module's answer to {@link PostSearch} (§8.4: Posts answer logged
 * out; §9.4: nothing authored by an under-18 does).
 *
 * <p>Both rules arrive by reusing {@link PostService#visibleTo} wholesale
 * rather than restating them: a Post is searchable by exactly the people who
 * could already read it, which is what §8.5 means by derived data never
 * exceeding the Dial. The author is handed over as a display name only — a Post
 * row is about the Post, and search has exactly one people surface (§8.5).
 */
@Component
class PostsByBody implements PostSearch {

    /** FTS narrows, then visibility narrows again — fetch more than are shown. */
    private static final int CANDIDATE_FACTOR = 5;

    private final PostRepository posts;
    private final PostService postService;
    private final ProfileService profiles;
    private final Clock clock;

    PostsByBody(PostRepository posts, PostService postService, ProfileService profiles,
                Clock clock) {
        this.posts = posts;
        this.postService = postService;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hit> matching(String tsquery, Optional<Member> viewer, int limit) {
        DateTimeFormatter when = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone());
        return posts.bodyCandidates(tsquery, limit * CANDIDATE_FACTOR).stream()
                .filter(post -> postService.visibleTo(post, viewer))
                .limit(limit)
                .map(post -> new Hit(post.id(), PostBodies.excerpt(post.body()),
                        profiles.cardFor(post.authorId()).displayName(),
                        when.format(post.createdAt())))
                .toList();
    }
}
