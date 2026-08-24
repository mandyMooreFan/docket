package com.mbeebe.docket.feed;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.5 held against §5.6: replies to you — to your Post, or to your
 * Reply in a thread you joined — surface in-app as a quiet section at the top
 * of the feed, listing the words themselves. Never a badge, a dot or a number
 * (§5.6 — §7.4's inbox count is the one exception, and it isn't this), never
 * an email. The feed never comes to get you.
 */
class NotificationTests extends FeedTestBase {

    private void replyAs(Cookie session, long postId, String body) throws Exception {
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/posts/" + postId + "/replies").cookie(session)
                        .param("body", body))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void aReplyToYourPostWaitsQuietlyAtTheTopOfYourFeedUntilYouVisit() throws Exception {
        Cookie author = completeMember("feed-notice-author@example.org", "Notified Author");
        Cookie friend = completeMember("feed-notice-friend@example.org", "Replying Friend");
        connect(author, friend);
        long postId = compose(author, "A post that will gather a reply.");
        replyAs(friend, postId, "The reply the author should hear about.");

        String feed = feedSeenBy(author);
        assertThat(feed).contains("Replies to you");
        assertThat(feed).contains("Replying Friend");
        assertThat(feed).contains("The reply the author should hear about.");
        assertThat(feed).contains("/posts/" + postId);
        // §5.6: no unread badge, no dot, no digit in the nav — the section IS the surface.
        assertThat(feed).doesNotContainIgnoringCase("unread");

        // The visit is the acknowledgement: the section empties, nothing nags twice.
        assertThat(feedSeenBy(author)).doesNotContain("Replies to you");
    }

    @Test
    void aReplyInAThreadYouJoinedReachesYouButYourOwnNever() throws Exception {
        Cookie owner = completeMember("feed-notice-owner@example.org", "Thread Owner Two");
        Cookie joiner = completeMember("feed-notice-joiner@example.org", "Thread Joiner");
        Cookie later = completeMember("feed-notice-later@example.org", "Later Voice");
        connect(owner, joiner);
        connect(owner, later);
        long postId = compose(owner, "A thread that gathers three voices.");

        replyAs(joiner, postId, "Joining words from the joiner.");
        // The joiner's feed view swallows everything so far.
        feedSeenBy(joiner);

        replyAs(later, postId, "Later words the joiner should hear about.");
        String feed = feedSeenBy(joiner);
        assertThat(feed).contains("Replies to you");
        assertThat(feed).contains("Later words the joiner should hear about.");
        // Your own Reply is not news to you.
        assertThat(feed).doesNotContain("Joining words from the joiner.");

        // §5.5: that is the entire list — a mere bystander connection hears nothing.
        Cookie bystander = completeMember("feed-notice-bystander@example.org", "By Stander");
        connect(owner, bystander);
        replyAs(later, postId, "One more line for the thread.");
        String bystanderFeed = feedSeenBy(bystander);
        assertThat(bystanderFeed).doesNotContain("Replies to you");
    }

    @Test
    void aRemovedReplyNeverReachesTheNotice() throws Exception {
        Cookie author = completeMember("feed-notice-rm@example.org", "Curating Author");
        Cookie friend = completeMember("feed-notice-rmf@example.org", "Removed Voice");
        connect(author, friend);
        long postId = compose(author, "A thread curated before it is read.");
        replyAs(friend, postId, "Words removed before the author's next visit.");

        // The author removes it from the Post page without visiting the feed.
        String page = mvc.perform(org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders.get("/posts/" + postId).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern
                .compile("/posts/" + postId + "/replies/(\\d+)/remove").matcher(page);
        assertThat(matcher.find()).isTrue();
        mvc.perform(post(matcher.group(0)).cookie(author))
                .andExpect(status().is3xxRedirection());

        assertThat(feedSeenBy(author)).doesNotContain("Words removed before");
    }
}
