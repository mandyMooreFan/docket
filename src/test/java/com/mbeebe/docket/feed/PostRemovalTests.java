package com.mbeebe.docket.feed;

import com.mbeebe.docket.moderation.TargetKind;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3 rung 1 for the feed's two reportable kinds: a removed Post or
 * Reply stops rendering on every surface that carried it, and a restored one
 * comes back on all of them (§10.5's reversible hold uses the same fact).
 *
 * <p>The Postgres container and GreenMail are shared across the whole run, so
 * every email here is prefixed "rm-feed-" and every assertion is member-scoped.
 */
class PostRemovalTests extends FeedTestBase {

    private static final Pattern IMAGE_URL = Pattern.compile("/images/(\\d+)");

    @Autowired
    PostReportable removals;

    private void remove(TargetKind kind, long id) {
        clock.advance(Duration.ofMinutes(1));
        assertThat(removals.remove(kind, id, clock.instant())).isTrue();
    }

    private String profilePageOf(Cookie viewer, long memberId) throws Exception {
        return mvc.perform(get("/p/" + memberId).cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String savedPageSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/saved").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aRemovedPostLeavesTheFeedItsOwnPageTheAuthorsProfileAndTheSavedList()
            throws Exception {
        Cookie author = completeMember("rm-feed-a1@example.org", "Auda Author");
        Cookie reader = completeMember("rm-feed-r1@example.org", "Reeda Reader");
        connect(author, reader);
        long postId = compose(author, "The words that were reported.");
        mvc.perform(post("/posts/" + postId + "/save").cookie(reader))
                .andExpect(status().is3xxRedirection());

        assertThat(feedSeenBy(reader)).contains("The words that were reported.");
        assertThat(savedPageSeenBy(reader)).contains("The words that were reported.");
        assertThat(profilePageOf(reader, memberId(author)))
                .contains("The words that were reported.");

        remove(TargetKind.POST, postId);

        // The feed's read position has already moved past it, so this asks the
        // author's Profile — where §5.1 says older Posts are reached — and the
        // three surfaces that do not depend on a high-water mark.
        assertThat(profilePageOf(reader, memberId(author)))
                .doesNotContain("The words that were reported.");
        assertThat(savedPageSeenBy(reader)).doesNotContain("The words that were reported.");
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isNotFound());
        // Its author is told by a statement of reasons, not by the page: for them
        // it is a 404 like anybody else's (§10.3 refuses covert half-states).
        mvc.perform(get("/posts/" + postId).cookie(author)).andExpect(status().isNotFound());
    }

    @Test
    void aRemovedPostNeverEntersAConnectionsFeed() throws Exception {
        Cookie author = completeMember("rm-feed-a2@example.org", "Fedde Author");
        Cookie reader = completeMember("rm-feed-r2@example.org", "Fedde Reader");
        connect(author, reader);
        long postId = compose(author, "A feed entry withdrawn before it was seen.");

        remove(TargetKind.POST, postId);

        assertThat(feedSeenBy(reader))
                .doesNotContain("A feed entry withdrawn before it was seen.");
    }

    @Test
    void aRemovedPostTakesItsImagesWithIt() throws Exception {
        Cookie author = completeMember("rm-feed-a3@example.org", "Imogen Author");
        byte[] png = "removed-post-bytes".getBytes(StandardCharsets.UTF_8);
        clock.advance(Duration.ofMinutes(1));
        String redirect = mvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("images", "shot.png", "image/png", png))
                        .param("body", "A picture that was reported.")
                        .cookie(author))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        long postId = Long.parseLong(redirect.substring(redirect.lastIndexOf('/') + 1));
        String page = mvc.perform(get(redirect).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = IMAGE_URL.matcher(page);
        assertThat(matcher.find()).isTrue();
        long imageId = Long.parseLong(matcher.group(1));
        mvc.perform(get("/images/" + imageId).cookie(author)).andExpect(status().isOk());

        remove(TargetKind.POST, postId);

        // PostImageAudience defers to PostService.visibleTo, so the bytes 404 as a
        // consequence of the Post's removal rather than by a second rule (§8.5).
        mvc.perform(get("/images/" + imageId).cookie(author)).andExpect(status().isNotFound());
    }

    @Test
    void aRemovedReplyLeavesItsThread() throws Exception {
        Cookie author = completeMember("rm-feed-a4@example.org", "Threda Author");
        Cookie replier = completeMember("rm-feed-r4@example.org", "Replia Friend");
        connect(author, replier);
        long postId = compose(author, "A thread with something reported in it.");
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/posts/" + postId + "/replies").cookie(replier)
                        .param("body", "The reply that was reported."))
                .andExpect(status().is3xxRedirection());
        long replyId = replyIdOn(author, postId);

        assertThat(feedSeenBy(author)).contains("The reply that was reported.");
        remove(TargetKind.REPLY, replyId);

        String thread = mvc.perform(get("/posts/" + postId).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(thread).doesNotContain("The reply that was reported.");
    }

    @Test
    void aRemovedPostTakesItsReplyNoticesOutOfRepliesToYou() throws Exception {
        Cookie author = completeMember("rm-feed-a5@example.org", "Notica Author");
        Cookie replier = completeMember("rm-feed-r5@example.org", "Notica Friend");
        connect(author, replier);
        long postId = compose(author, "A thread whose Post was reported.");
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/posts/" + postId + "/replies").cookie(replier)
                        .param("body", "A notice that should not survive."))
                .andExpect(status().is3xxRedirection());

        remove(TargetKind.POST, postId);

        assertThat(feedSeenBy(author)).doesNotContain("A notice that should not survive.");
    }

    @Test
    void aRestoredPostIsBackOnEverySurfaceItLeft() throws Exception {
        Cookie author = completeMember("rm-feed-a6@example.org", "Restora Author");
        Cookie reader = completeMember("rm-feed-r6@example.org", "Restora Reader");
        connect(author, reader);
        long postId = compose(author, "Held while the queue looked at it.");
        mvc.perform(post("/posts/" + postId + "/save").cookie(reader))
                .andExpect(status().is3xxRedirection());

        remove(TargetKind.POST, postId);
        assertThat(savedPageSeenBy(reader)).doesNotContain("Held while the queue looked at it.");

        assertThat(removals.restore(TargetKind.POST, postId)).isTrue();

        assertThat(savedPageSeenBy(reader)).contains("Held while the queue looked at it.");
        assertThat(profilePageOf(reader, memberId(author)))
                .contains("Held while the queue looked at it.");
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isOk());
    }

    @Test
    void aRestoredReplyIsBackInItsThread() throws Exception {
        Cookie author = completeMember("rm-feed-a7@example.org", "Backagain Author");
        Cookie replier = completeMember("rm-feed-r7@example.org", "Backagain Friend");
        connect(author, replier);
        long postId = compose(author, "A thread that got its reply back.");
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/posts/" + postId + "/replies").cookie(replier)
                        .param("body", "A reply held and then let go."))
                .andExpect(status().is3xxRedirection());
        long replyId = replyIdOn(author, postId);

        remove(TargetKind.REPLY, replyId);
        assertThat(threadPageOf(author, postId)).doesNotContain("A reply held and then let go.");

        assertThat(removals.restore(TargetKind.REPLY, replyId)).isTrue();
        assertThat(threadPageOf(author, postId)).contains("A reply held and then let go.");
    }

    @Test
    void theFeedAnswersForPostsAndRepliesAndForNoOtherKind() throws Exception {
        Cookie author = completeMember("rm-feed-a8@example.org", "Ownkind Author");
        long postId = compose(author, "Owned by the feed module.");

        assertThat(removals.forModeration(TargetKind.POST, postId)).isPresent();
        // Not mine: the registry must be free to fall through to the module that
        // does own the kind, so these are empty rather than refusals.
        assertThat(removals.forModeration(TargetKind.MESSAGE, postId)).isEmpty();
        assertThat(removals.forModeration(TargetKind.COMPANY, postId)).isEmpty();
        assertThat(removals.forModeration(TargetKind.PROFILE, postId)).isEmpty();
        assertThat(removals.remove(TargetKind.JOB_POSTING, postId, clock.instant())).isFalse();
        assertThat(removals.restore(TargetKind.JOB_POSTING, postId)).isFalse();
    }

    @Test
    void aPostOutOfTheDialIsNotReportableButTheQueueStillSeesIt() throws Exception {
        Cookie author = completeMember("rm-feed-a9@example.org", "Dialled Author");
        long postId = compose(author, "Kept among connections only.");
        mvc.perform(post("/profile/dial").cookie(author).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        assertThat(removals.visibleToReporter(TargetKind.POST, postId, Optional.empty()))
                .isEmpty();
        // The queue is not the reporter: it must be able to judge what it cannot see.
        assertThat(removals.forModeration(TargetKind.POST, postId))
                .get()
                .satisfies(item -> {
                    assertThat(item.summary()).isEqualTo("Kept among connections only.");
                    assertThat(item.href()).isEqualTo("/posts/" + postId);
                    assertThat(item.authorId()).contains(memberId(author));
                    assertThat(item.removed()).isFalse();
                });
    }

    @Test
    void anItemThatDoesNotExistIsEmptyEvenForTheQueue() {
        assertThat(removals.forModeration(TargetKind.POST, 987_654_321L)).isEmpty();
        assertThat(removals.forModeration(TargetKind.REPLY, 987_654_321L)).isEmpty();
    }

    @Test
    void removingTheSamePostTwiceKeepsTheFirstRemovalsDate() throws Exception {
        Cookie author = completeMember("rm-feed-a10@example.org", "Twicea Author");
        long postId = compose(author, "Removed once, asked for twice.");

        remove(TargetKind.POST, postId);
        remove(TargetKind.POST, postId);

        assertThat(removals.forModeration(TargetKind.POST, postId))
                .get().satisfies(item -> assertThat(item.removed()).isTrue());
        mvc.perform(get("/posts/" + postId).cookie(author)).andExpect(status().isNotFound());
    }

    /** The remove button's own form carries the Reply's id — the only place it renders. */
    private long replyIdOn(Cookie author, long postId) throws Exception {
        String page = threadPageOf(author, postId);
        Matcher matcher = Pattern.compile("/posts/" + postId + "/replies/(\\d+)/remove")
                .matcher(page);
        if (!matcher.find()) {
            throw new AssertionError("No reply on the thread page for post " + postId);
        }
        return Long.parseLong(matcher.group(1));
    }

    private String threadPageOf(Cookie viewer, long postId) throws Exception {
        return mvc.perform(get("/posts/" + postId).cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
