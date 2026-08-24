package com.mbeebe.docket.jobs;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.2.2: the job-attached Post — a member writes a Post and attaches
 * one of the board's open postings, the ONLY path from board to feed; nothing
 * auto-syndicates. Plus §2.3's rail: "Jobs from your network" — open postings
 * at companies where you have a Connection, derived, unranked.
 */
class JobAttachedPostTests extends JobsTestBase {

    private static final Pattern POST_URL = Pattern.compile("/posts/(\\d+)");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PostingCloser closer;

    @Test
    void aMemberWritesAPostAndAttachesAnOpenPosting() throws Exception {
        Cookie poster = posterAt("jobs-share-poster@example.org", "Sha Poster",
                "Shareworks Ltd", "shareworks-sh.example");
        long posting = postJob(poster, companies.named("Shareworks Ltd").id(),
                "Quilted Analyst");

        // Not signed in: nothing to attach with.
        mvc.perform(post("/jobs/" + posting + "/share").param("body", "Come work with us."))
                .andExpect(status().is3xxRedirection());

        // Incomplete: a job-attached Post is still a Post (§3.2).
        Cookie incomplete = signUpAndIn("jobs-share-incomplete@example.org");
        mvc.perform(post("/jobs/" + posting + "/share").cookie(incomplete)
                        .param("body", "Come work with us."))
                .andExpect(status().isForbidden());

        // A Post needs words — attaching is not reposting (§5.2.2, §5.6).
        mvc.perform(post("/jobs/" + posting + "/share").cookie(poster).param("body", " "))
                .andExpect(status().isUnprocessableEntity());

        clock.advance(Duration.ofMinutes(1));
        String redirect = mvc.perform(post("/jobs/" + posting + "/share").cookie(poster)
                        .param("body", "We're hiring a quilted analyst — ask me anything."))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        Matcher matcher = POST_URL.matcher(redirect);
        assertThat(matcher.find()).isTrue();
        long postId = Long.parseLong(matcher.group(1));

        // The stored fact: a Post of the third kind, carrying its reference.
        assertThat(jdbc.queryForObject("""
                select kind from post where id = %d and job_posting_id = %d
                """.formatted(postId, posting), String.class)).isEqualTo("JOB_ATTACHED");

        // The Post's page renders the member's words AND the compact card:
        // title, company, salary, a link to the posting.
        String page = mvc.perform(get("/posts/" + postId).cookie(poster))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("ask me anything");
        assertThat(page).contains("Quilted Analyst");
        assertThat(page).contains("Shareworks Ltd");
        assertThat(page).contains("£45,000–£60,000");
        assertThat(page).contains("/jobs/" + posting);

        // A Connection's feed carries it, card and all (§5.1 distribution).
        Cookie reader = completeMember("jobs-share-reader@example.org", "Rita Reader");
        connect(reader, poster);
        String feed = mvc.perform(get("/").cookie(reader)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(feed).contains("ask me anything");
        assertThat(feed).contains("Quilted Analyst");
        assertThat(feed).contains("/jobs/" + posting);
    }

    @Test
    void onlyAnOpenPostingCanBeAttached() throws Exception {
        Cookie poster = posterAt("jobs-share-closed@example.org", "Clo Sharer",
                "Closedshare Ltd", "closedshare-sh.example");
        long posting = postJob(poster, companies.named("Closedshare Ltd").id(),
                "Fleeting Role");
        clock.advance(Duration.ofDays(31));
        closer.closeDue(clock.instant());

        mvc.perform(post("/jobs/" + posting + "/share").cookie(poster)
                        .param("body", "Too late to say this."))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theRailShowsOpenPostingsWhereYouHaveAConnection() throws Exception {
        Cookie poster = posterAt("jobs-rail-poster@example.org", "Rai Poster",
                "Railworks Ltd", "railworks-sh.example");
        postJob(poster, companies.named("Railworks Ltd").id(), "Signal Keeper");

        // Connected to the poster: the posting is at a company where a
        // Connection holds a current Position — it appears in the rail.
        Cookie connected = completeMember("jobs-rail-connected@example.org", "Con Nected");
        connect(connected, poster);
        String feed = mvc.perform(get("/").cookie(connected)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(feed).contains("Signal Keeper");

        // A member with no Connection there: the honest empty panel (§13.4).
        Cookie stranger = completeMember("jobs-rail-stranger@example.org", "Stra Nger");
        String strangerFeed = mvc.perform(get("/").cookie(stranger)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(strangerFeed).doesNotContain("Signal Keeper");
        assertThat(strangerFeed).contains("No jobs from your network yet.");
    }
}
