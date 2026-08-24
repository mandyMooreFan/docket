package com.mbeebe.docket.feed;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.3: the private Save. A Member's own bookmark of a Post, visible
 * to nobody else, listed on their /saved page — and counted nowhere (§5.6
 * refuses save counts).
 */
class SaveTests extends FeedTestBase {

    String savedPageSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/saved").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aSaveIsPrivateAndListsOnlyOnYourOwnSavedPage() throws Exception {
        Cookie author = completeMember("feed-save-author@example.org", "Saved Author");
        Cookie saver = completeMember("feed-save-saver@example.org", "Quiet Saver");
        connect(author, saver);
        long postId = compose(author, "Words worth keeping for later.");

        mvc.perform(post("/posts/" + postId + "/save").cookie(saver))
                .andExpect(status().is3xxRedirection());

        assertThat(savedPageSeenBy(saver)).contains("Words worth keeping for later.");
        // Private: the author's own /saved shows nothing, and their view of the
        // Post carries no trace of who saved it — no count exists to leak (§5.6).
        assertThat(savedPageSeenBy(author)).doesNotContain("Words worth keeping for later.");
        String authorsView = mvc.perform(get("/posts/" + postId).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(authorsView).doesNotContain("Quiet Saver");
        assertThat(authorsView).doesNotContainIgnoringCase("1 save");
    }

    @Test
    void unsaveLetsGoAndSavingTwiceIsQuiet() throws Exception {
        Cookie author = completeMember("feed-save-un@example.org", "Unsave Author");
        Cookie saver = completeMember("feed-save-uns@example.org", "Unsave Saver");
        connect(author, saver);
        long postId = compose(author, "Held for a while, then let go.");

        mvc.perform(post("/posts/" + postId + "/save").cookie(saver))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/posts/" + postId + "/save").cookie(saver))
                .andExpect(status().is3xxRedirection());
        assertThat(savedPageSeenBy(saver)).contains("Held for a while, then let go.");

        mvc.perform(post("/posts/" + postId + "/unsave").cookie(saver))
                .andExpect(status().is3xxRedirection());
        String saved = savedPageSeenBy(saver);
        assertThat(saved).doesNotContain("Held for a while, then let go.");
        assertThat(saved).contains("Nothing saved yet");
    }

    @Test
    void onlyAPostYouCanSeeCanBeSaved() throws Exception {
        Cookie author = completeMember("feed-save-dial@example.org", "Dialled Author");
        Cookie stranger = completeMember("feed-save-str@example.org", "Saving Stranger");
        long postId = compose(author, "Kept among connections only.");
        mvc.perform(post("/profile/dial").cookie(author).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/posts/" + postId + "/save").cookie(stranger))
                .andExpect(status().isNotFound());
    }
}
