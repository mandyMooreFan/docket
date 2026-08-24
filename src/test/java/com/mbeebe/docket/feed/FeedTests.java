package com.mbeebe.docket.feed;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.1: distribution is the mutual graph and nothing else,
 * reverse-chronological, no ranking — and the feed ends. The read position is
 * per-member tracked state: nothing re-surfaces, nothing shows twice.
 */
class FeedTests extends FeedTestBase {

    @Test
    void theFeedIsTheMutualGraphNewestFirstAndItEnds() throws Exception {
        Cookie reader = completeMember("feed-flow-reader@example.org", "Reader Flow");
        Cookie friend = completeMember("feed-flow-friend@example.org", "Friend Flow");
        Cookie stranger = completeMember("feed-flow-stranger@example.org", "Stranger Flow");
        connect(reader, friend);

        compose(stranger, "Stranger words that must never travel.");
        compose(friend, "Older words from a connection.");
        compose(friend, "Newer words from a connection.");
        compose(reader, "The reader writing for their own connections.");

        String feed = feedSeenBy(reader);
        // §5.1: a stranger's Post can NEVER reach this feed.
        assertThat(feed).doesNotContain("Stranger words that must never travel.");
        // The feed is what your connections write — not an echo of yourself.
        assertThat(feed).doesNotContain("The reader writing for their own connections.");
        // Reverse-chronological, then the hard boundary, then nothing.
        int newer = feed.indexOf("Newer words from a connection.");
        int older = feed.indexOf("Older words from a connection.");
        int end = feed.indexOf("You&#39;re caught up.");
        if (end < 0) {
            end = feed.indexOf("You're caught up.");
        }
        assertThat(newer).isGreaterThan(-1);
        assertThat(older).isGreaterThan(newer);
        assertThat(end).isGreaterThan(older);
    }

    @Test
    void aViewedPostNeverShowsTwiceAndNeverResurfaces() throws Exception {
        Cookie reader = completeMember("feed-once-reader@example.org", "Reader Once");
        Cookie friend = completeMember("feed-once-friend@example.org", "Friend Once");
        connect(reader, friend);

        long first = compose(friend, "The first dispatch, read exactly once.");
        assertThat(feedSeenBy(reader)).contains("The first dispatch, read exactly once.");

        // The next visit starts past it: the boundary, and nothing above it twice.
        String second = feedSeenBy(reader);
        assertThat(second).doesNotContain("The first dispatch, read exactly once.");
        assertThat(second).contains("caught up");

        // A newer Post appears alone; the old one stays behind the boundary.
        compose(friend, "The second dispatch, newer than the mark.");
        String third = feedSeenBy(reader);
        assertThat(third).contains("The second dispatch, newer than the mark.");
        assertThat(third).doesNotContain("The first dispatch, read exactly once.");

        // Older Posts are reached from the author's Profile instead (§5.1).
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/p/" + memberId(friend)).cookie(reader))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString(
                                "/posts/" + first)));
    }

    @Test
    void theEmptyGraphGetsTheHonestCopyNotAFallback() throws Exception {
        Cookie alone = completeMember("feed-empty@example.org", "Alone Empty");
        String feed = feedSeenBy(alone);
        assertThat(feed).contains("connections yet");
        assertThat(feed).contains("nothing gets put here for you");
        assertThat(feed).doesNotContain("caught up");
    }

    @Test
    void theRailCarriesPendingRequestsAndTheHonestJobsState() throws Exception {
        Cookie reader = completeMember("feed-rail-reader@example.org", "Rail Reader");
        Cookie asker = completeMember("feed-rail-asker@example.org", "Rail Asker");
        mvc.perform(post("/p/" + memberId(reader) + "/connect").cookie(asker))
                .andExpect(status().is3xxRedirection());

        String feed = feedSeenBy(reader);
        assertThat(feed).contains("Pending requests");
        assertThat(feed).contains("Rail Asker");
        // §13.4: honest about the jobs board not existing yet (#35).
        assertThat(feed).contains("No jobs from your network yet.");

        mvc.perform(post("/network/accept/" + memberId(asker)).cookie(reader))
                .andExpect(status().is3xxRedirection());
        assertThat(feedSeenBy(reader)).contains("No pending requests.");
    }

    @Test
    void aBlockErasesTheirPostsFromTheFeedEntirely() throws Exception {
        Cookie reader = completeMember("feed-block-reader@example.org", "Block Reader");
        Cookie kept = completeMember("feed-block-kept@example.org", "Kept Friend");
        Cookie blocked = completeMember("feed-block-gone@example.org", "Gone Friend");
        connect(reader, kept);
        connect(reader, blocked);

        compose(kept, "Words from the connection who stays.");
        compose(blocked, "Words that vanish behind the block.");
        mvc.perform(post("/p/" + memberId(blocked) + "/block").cookie(reader))
                .andExpect(status().is3xxRedirection());

        String feed = feedSeenBy(reader);
        assertThat(feed).contains("Words from the connection who stays.");
        assertThat(feed).doesNotContain("Words that vanish behind the block.");
    }
}
