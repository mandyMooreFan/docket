package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.AgeRollover;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §9.4 — the compliance heart of #33. Nothing authored by an under-18
 * is ever visible logged-out or indexed: Posts are members-only regardless of
 * the Dial, Replies are omitted from logged-out rendering with no placeholder,
 * counts count only what the view shows — and the 18 rollover never lifts any
 * of it, because authored-as-minor is fixed at write time (§9.3 deletes the
 * birth data the derivation would need).
 */
class MinorAuthoredTests extends FeedTestBase {

    @Autowired
    AgeRollover rollover;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aMinorsPostIsMembersOnlyRegardlessOfTheDialAndTheRolloverNeverLiftsIt()
            throws Exception {
        Cookie minor = completeMinor("feed-minor-poster@example.org", "Minor Poster");
        long minorId = memberId(minor);
        // The Dial says PUBLIC; §9.4 caps it anyway.
        mvc.perform(post("/profile/dial").cookie(minor).param("dial", "PUBLIC"))
                .andExpect(status().is3xxRedirection());
        long cappedPost = compose(minor, "Words written at seventeen, capped for good.");

        // Members-only: a signed-in member sees it, the logged-out web does not.
        Cookie member = completeMember("feed-minor-viewer@example.org", "Adult Viewer");
        mvc.perform(get("/posts/" + cappedPost).cookie(member))
                .andExpect(status().isOk());
        mvc.perform(get("/posts/" + cappedPost))
                .andExpect(status().isNotFound());

        // The 18 rollover: the member becomes an adult, their PUBLIC Profile opens up...
        rollover.rolloverDueMinors(YearMonth.now(clock).plusYears(2));
        String publicProfile = mvc.perform(get("/p/" + minorId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // ...but the Post authored as a minor stays members-only, permanently:
        // absent from the logged-out Profile list with no placeholder, 404 at
        // its address, and the stored fact untouched by the rollover.
        assertThat(publicProfile).doesNotContain("Words written at seventeen");
        assertThat(publicProfile).doesNotContain("/posts/" + cappedPost);
        mvc.perform(get("/posts/" + cappedPost))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject(
                "select authored_as_minor from post where id = " + cappedPost, Boolean.class))
                .isTrue();

        // Delete-and-repost as an adult is the only release: a new Post is open.
        long adultPost = compose(minor, "Words written at eighteen, on the open web.");
        mvc.perform(get("/posts/" + adultPost))
                .andExpect(status().isOk());
        // Members still read the capped one.
        mvc.perform(get("/posts/" + cappedPost).cookie(member))
                .andExpect(status().isOk());
    }

    @Test
    void aMinorsReplyIsOmittedLoggedOutWithNoPlaceholderAndTheCountAgrees()
            throws Exception {
        Cookie author = completeMember("feed-minor-thread@example.org", "Thread Owner");
        Cookie adult = completeMember("feed-minor-adult@example.org", "Adult Replier");
        Cookie minor = completeMinor("feed-minor-replier@example.org", "Minor Replier");
        long postId = compose(author, "A public thread with mixed repliers.");
        connect(adult, author);
        // §9.2: the minor may send a request to anyone; the adult cannot reach them.
        connect(minor, author);

        mvc.perform(post("/posts/" + postId + "/replies").cookie(adult)
                        .param("body", "Adult words that stay on the open page."))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/posts/" + postId + "/replies").cookie(minor)
                        .param("body", "Minor words that stay members-only."))
                .andExpect(status().is3xxRedirection());

        // Signed in: both replies, and the count says two.
        String memberView = mvc.perform(get("/posts/" + postId).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(memberView).contains("Adult words that stay on the open page.");
        assertThat(memberView).contains("Minor words that stay members-only.");
        assertThat(memberView).contains("2 replies");

        // Logged out: the minor's Reply is omitted — no placeholder, no name —
        // and the count counts only what this view shows (§9.4).
        String loggedOut = mvc.perform(get("/posts/" + postId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(loggedOut).contains("Adult words that stay on the open page.");
        assertThat(loggedOut).doesNotContain("Minor words that stay members-only.");
        assertThat(loggedOut).doesNotContain("Minor Replier");
        assertThat(loggedOut).contains("1 reply");
        assertThat(loggedOut).doesNotContain("2 replies");
    }
}
