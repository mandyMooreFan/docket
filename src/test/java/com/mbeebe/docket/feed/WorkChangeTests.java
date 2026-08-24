package com.mbeebe.docket.feed;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.2.3: opt-in work changes — generated only when the member ticks
 * the box at the moment of the Profile edit. Never automatic, never
 * retroactive, only genuine news: started a role, left a role.
 */
class WorkChangeTests extends FeedTestBase {

    private void addPosition(Cookie session, String title, String company,
                             boolean share) throws Exception {
        clock.advance(Duration.ofMinutes(1));
        var request = post("/profile/positions").cookie(session)
                .param("title", title).param("company", company)
                .param("startMonth", "3").param("startYear", "2024")
                .param("description", "");
        if (share) {
            request = request.param("share", "on");
        }
        mvc.perform(request).andExpect(status().is3xxRedirection());
    }

    @Test
    void tickingTheBoxAtTheEditSharesGenuineNewsAndNothingElseDoes() throws Exception {
        Cookie mover = completeMember("feed-work-mover@example.org", "Job Mover");
        Cookie friend = completeMember("feed-work-friend@example.org", "Watching Friend");
        connect(mover, friend);

        // Unticked: the edit stays an edit — nothing reaches the feed.
        addPosition(mover, "Quiet Analyst", "Unshared House", false);
        assertThat(feedSeenBy(friend)).doesNotContain("Quiet Analyst");

        // Ticked: a work-change Post, in the connections' feed and on the Profile.
        addPosition(mover, "Loud Engineer", "Shared Works", true);
        String feed = feedSeenBy(friend);
        assertThat(feed).contains("Started as Loud Engineer at Shared Works");
        // Never retroactive: the earlier unticked edit did not ride along.
        assertThat(feed).doesNotContain("Quiet Analyst");

        String profile = mvc.perform(get("/p/" + memberId(mover)).cookie(friend))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(profile).contains("Started as Loud Engineer at Shared Works");
    }

    @Test
    void endingARoleSharesOnlyWhenTicked() throws Exception {
        Cookie leaver = completeMember("feed-work-leaver@example.org", "Role Leaver");
        Cookie friend = completeMember("feed-work-watcher@example.org", "Leaving Watcher");
        connect(leaver, friend);

        addPosition(leaver, "Departing Director", "Farewell Bureau", false);
        long positionId = latestPositionOf(memberId(leaver));

        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/profile/positions/" + positionId + "/end").cookie(leaver)
                        .param("endMonth", "6").param("endYear", "2026")
                        .param("share", "on"))
                .andExpect(status().is3xxRedirection());
        assertThat(feedSeenBy(friend)).contains("Left Departing Director at Farewell Bureau");

        // And the unticked ending, on another role, stays quiet.
        addPosition(leaver, "Silent Steward", "Hushed Hall", false);
        long silentId = latestPositionOf(memberId(leaver));
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(post("/profile/positions/" + silentId + "/end").cookie(leaver)
                        .param("endMonth", "7").param("endYear", "2026"))
                .andExpect(status().is3xxRedirection());
        assertThat(feedSeenBy(friend)).doesNotContain("Left Silent Steward");
    }

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    private long latestPositionOf(long memberId) {
        return jdbc.queryForObject(
                "select id from position where member_id = " + memberId
                        + " order by id desc limit 1", Long.class);
    }
}
