package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.ProfilePostView;
import com.mbeebe.docket.profile.ProfilePostsLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * §5.4: where a Post lives — the author's Profile as a dated list, riding the
 * Profile's single Dial (the page gates that before asking here). The one rule
 * this seam adds is §9.4's: a logged-out rendering omits minor-authored Posts
 * entirely, with no placeholder, permanently. A removed Post (§10.3 rung 1) is
 * absent from the list for everyone, the owner included — the repository's own
 * predicate, so it cannot be forgotten here.
 */
@Service
public class ProfilePosts implements ProfilePostsLookup {

    private final PostRepository posts;
    private final Clock clock;

    ProfilePosts(PostRepository posts, Clock clock) {
        this.posts = posts;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfilePostView> postsOn(long ownerId, Optional<Member> viewer) {
        DateTimeFormatter when = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone());
        return posts.findByAuthorIdAndRemovedAtIsNullOrderByCreatedAtDescIdDesc(ownerId).stream()
                .filter(post -> viewer.isPresent() || !post.authoredAsMinor())
                .map(post -> new ProfilePostView(post.id(),
                        when.format(post.createdAt()), PostBodies.excerpt(post.body())))
                .toList();
    }
}
