package com.mbeebe.docket.feed;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.3: Replies and a private Save are the complete interaction list.
 * Replies are limited to the author's Connections even on a public Post; the
 * author may remove any Reply and may close the thread; the reply count is the
 * only visible number, derived per viewer.
 */
class ReplyTests extends FeedTestBase {

    String postPageSeenBy(Cookie session, long postId) throws Exception {
        return mvc.perform(get("/posts/" + postId).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aConnectionRepliesAndTheCountIsTheOnlyNumber() throws Exception {
        Cookie author = completeMember("feed-reply-author@example.org", "Reply Author");
        Cookie friend = completeMember("feed-reply-friend@example.org", "Reply Friend");
        connect(author, friend);
        long postId = compose(author, "Something worth a sentence back.");

        mvc.perform(post("/posts/" + postId + "/replies").cookie(friend)
                        .param("body", "A whole sentence, as the filter intends."))
                .andExpect(status().is3xxRedirection());

        String page = postPageSeenBy(author, postId);
        assertThat(page).contains("A whole sentence, as the filter intends.");
        assertThat(page).contains("Reply Friend");
        assertThat(page).contains("1 reply");

        // In the friend's feed the count is navigation on the entry.
        assertThat(feedSeenBy(friend)).contains("1 reply");
    }

    @Test
    void repliesAreForTheAuthorsConnectionsEvenOnAPublicPost() throws Exception {
        Cookie author = completeMember("feed-reply-pub@example.org", "Public Author");
        Cookie stranger = completeMember("feed-reply-str@example.org", "Passing Stranger");
        long postId = compose(author, "Public words a stranger may read.");

        // The stranger can read the public Post but gets no reply affordance.
        String page = postPageSeenBy(stranger, postId);
        assertThat(page).doesNotContain("name=\"body\"");
        assertThat(page).contains("connections");

        // And the server refuses, not just the template (§5.3).
        mvc.perform(post("/posts/" + postId + "/replies").cookie(stranger)
                        .param("body", "Wedging in from outside."))
                .andExpect(status().isForbidden());
        assertThat(postPageSeenBy(author, postId))
                .doesNotContain("Wedging in from outside.");
    }

    @Test
    void theAuthorMayRemoveAnyReplyAndNobodyElseMay() throws Exception {
        Cookie author = completeMember("feed-reply-rm@example.org", "Removing Author");
        Cookie friend = completeMember("feed-reply-rmf@example.org", "Removed Friend");
        Cookie other = completeMember("feed-reply-rmo@example.org", "Other Friend");
        connect(author, friend);
        connect(author, other);
        long postId = compose(author, "A thread the author curates.");
        mvc.perform(post("/posts/" + postId + "/replies").cookie(friend)
                        .param("body", "Words the author will remove."))
                .andExpect(status().is3xxRedirection());
        String replyId = replyIdOn(author, postId);

        // Another Connection may not remove it.
        mvc.perform(post("/posts/" + postId + "/replies/" + replyId + "/remove")
                        .cookie(other))
                .andExpect(status().isNotFound());

        mvc.perform(post("/posts/" + postId + "/replies/" + replyId + "/remove")
                        .cookie(author))
                .andExpect(status().is3xxRedirection());
        String page = postPageSeenBy(author, postId);
        assertThat(page).doesNotContain("Words the author will remove.");
        assertThat(page).doesNotContain("1 reply");
    }

    @Test
    void theAuthorMayCloseTheThread() throws Exception {
        Cookie author = completeMember("feed-reply-close@example.org", "Closing Author");
        Cookie friend = completeMember("feed-reply-closef@example.org", "Closed-out Friend");
        connect(author, friend);
        long postId = compose(author, "The last word will be the author's.");

        // Only the author may close.
        mvc.perform(post("/posts/" + postId + "/close").cookie(friend))
                .andExpect(status().isNotFound());
        mvc.perform(post("/posts/" + postId + "/close").cookie(author))
                .andExpect(status().is3xxRedirection());

        assertThat(postPageSeenBy(friend, postId)).contains("closed");
        mvc.perform(post("/posts/" + postId + "/replies").cookie(friend)
                        .param("body", "Too late to add this."))
                .andExpect(status().isForbidden());
    }

    @Test
    void aBlockedPairSeeNothingOfEachOtherInAThread() throws Exception {
        Cookie author = completeMember("feed-reply-blk@example.org", "Thread Author");
        Cookie replier = completeMember("feed-reply-blkr@example.org", "Blocked Replier");
        Cookie viewer = completeMember("feed-reply-blkv@example.org", "Blocking Viewer");
        connect(author, replier);
        connect(author, viewer);
        long postId = compose(author, "A thread two people share uneasily.");
        mvc.perform(post("/posts/" + postId + "/replies").cookie(replier)
                        .param("body", "Words the viewer must not see."))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/p/" + memberId(replier) + "/block").cookie(viewer))
                .andExpect(status().is3xxRedirection());

        String page = postPageSeenBy(viewer, postId);
        assertThat(page).doesNotContain("Words the viewer must not see.");
        assertThat(page).doesNotContain("1 reply");
        // The author, outside the block, still sees the reply.
        assertThat(postPageSeenBy(author, postId))
                .contains("Words the viewer must not see.");
    }

    @Test
    void aReplyIsASentenceNotAnEssayAndNeverBlank() throws Exception {
        Cookie author = completeMember("feed-reply-val@example.org", "Valid Author");
        Cookie friend = completeMember("feed-reply-valf@example.org", "Valid Friend");
        connect(author, friend);
        long postId = compose(author, "Constraints make the form.");

        mvc.perform(post("/posts/" + postId + "/replies").cookie(friend)
                        .param("body", "   "))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/posts/" + postId + "/replies").cookie(friend)
                        .param("body", "x".repeat(2001)))
                .andExpect(status().isUnprocessableEntity());
    }

    private String replyIdOn(Cookie session, long postId) throws Exception {
        String page = postPageSeenBy(session, postId);
        var matcher = java.util.regex.Pattern
                .compile("/posts/" + postId + "/replies/(\\d+)/remove").matcher(page);
        if (!matcher.find()) {
            throw new AssertionError("No remove affordance on the page");
        }
        return matcher.group(1);
    }
}
