package com.mbeebe.docket.jobs;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.5: opt-in saved searches — off by default, created explicitly from
 * the current filter set, seeker-chosen frequency, one click to stop (a
 * tokenized link that works without login), contents limited to matching
 * postings since the last send. No "jobs you might like", ever.
 */
class SavedSearchTests extends JobsTestBase {

    private static final Pattern STOP_LINK =
            Pattern.compile("/jobs/searches/stop/([A-Za-z0-9_-]+)");

    @Autowired
    JobSearchMailer sender;

    private long postMarked(Cookie poster, long company, String title) throws Exception {
        clock.advance(Duration.ofMinutes(1));
        return postJob(poster, company, title);
    }

    @Test
    void theMailCarriesOnlyMatchingPostingsSinceTheLastSend() throws Exception {
        Cookie poster = posterAt("jobs-search-poster@example.org", "Sela Poster",
                "Searchworks Ltd", "searchworks-ss.example");
        long company = companies.named("Searchworks Ltd").id();

        // Creating a search takes a member.
        mvc.perform(post("/jobs/searches").param("q", "Xylo").param("frequency", "DAILY"))
                .andExpect(status().is3xxRedirection());

        Cookie seeker = completeMember("jobs-search-seeker@example.org", "Sia Seeker");
        mvc.perform(post("/jobs/searches").cookie(seeker)
                        .param("q", "Xylo").param("frequency", "DAILY"))
                .andExpect(status().is3xxRedirection());
        int mailsBefore = mailBodiesFor("jobs-search-seeker@example.org").size();

        postMarked(poster, company, "Xylo Wrangler");
        postMarked(poster, company, "Plain Wrangler");

        // Not due yet: daily means daily, not on every sweep.
        sender.runDue(clock.instant());
        assertThat(mailBodiesFor("jobs-search-seeker@example.org")).hasSize(mailsBefore);

        clock.advance(Duration.ofDays(1));
        sender.runDue(clock.instant());
        List<String> bodies = mailBodiesFor("jobs-search-seeker@example.org");
        assertThat(bodies).hasSize(mailsBefore + 1);
        String digest = bodies.get(bodies.size() - 1);
        assertThat(digest).contains("Xylo Wrangler");
        assertThat(digest).doesNotContain("Plain Wrangler");
        assertThat(digest).contains("/jobs/searches/stop/");

        // Nothing new since the last send: no mail at all — never an empty one.
        clock.advance(Duration.ofDays(1));
        sender.runDue(clock.instant());
        assertThat(mailBodiesFor("jobs-search-seeker@example.org")).hasSize(mailsBefore + 1);

        // Only what arrived since the last send, not the whole history again.
        postMarked(poster, company, "Xylo Curator");
        clock.advance(Duration.ofDays(1));
        sender.runDue(clock.instant());
        bodies = mailBodiesFor("jobs-search-seeker@example.org");
        assertThat(bodies).hasSize(mailsBefore + 2);
        String second = bodies.get(bodies.size() - 1);
        assertThat(second).contains("Xylo Curator");
        assertThat(second).doesNotContain("Xylo Wrangler");
    }

    @Test
    void oneClickStopsItWithoutLogin() throws Exception {
        Cookie poster = posterAt("jobs-stop-poster@example.org", "Stopa Poster",
                "Stopworks Ltd", "stopworks-ss.example");
        long company = companies.named("Stopworks Ltd").id();

        Cookie seeker = completeMember("jobs-stop-seeker@example.org", "Stef Seeker");
        mvc.perform(post("/jobs/searches").cookie(seeker)
                        .param("q", "Yonder").param("frequency", "DAILY"))
                .andExpect(status().is3xxRedirection());

        postMarked(poster, company, "Yonder Keeper");
        clock.advance(Duration.ofDays(1));
        sender.runDue(clock.instant());
        List<String> bodies = mailBodiesFor("jobs-stop-seeker@example.org");
        Matcher matcher = STOP_LINK.matcher(bodies.get(bodies.size() - 1));
        assertThat(matcher.find()).isTrue();

        // The one click, no login, no cookie.
        mvc.perform(get("/jobs/searches/stop/" + matcher.group(1)))
                .andExpect(status().isOk());

        int mailsAfterStop = mailBodiesFor("jobs-stop-seeker@example.org").size();
        postMarked(poster, company, "Yonder Warden");
        clock.advance(Duration.ofDays(1));
        sender.runDue(clock.instant());
        assertThat(mailBodiesFor("jobs-stop-seeker@example.org")).hasSize(mailsAfterStop);
    }

    @Test
    void weeklyMeansWeekly() throws Exception {
        Cookie poster = posterAt("jobs-week-poster@example.org", "Wes Poster",
                "Weekworks Ltd", "weekworks-ss.example");
        long company = companies.named("Weekworks Ltd").id();

        Cookie seeker = completeMember("jobs-week-seeker@example.org", "Willa Seeker");
        mvc.perform(post("/jobs/searches").cookie(seeker)
                        .param("q", "Zephyr").param("frequency", "WEEKLY"))
                .andExpect(status().is3xxRedirection());
        int mailsBefore = mailBodiesFor("jobs-week-seeker@example.org").size();

        postMarked(poster, company, "Zephyr Reader");
        clock.advance(Duration.ofDays(2));
        sender.runDue(clock.instant());
        assertThat(mailBodiesFor("jobs-week-seeker@example.org")).hasSize(mailsBefore);

        clock.advance(Duration.ofDays(6));
        sender.runDue(clock.instant());
        assertThat(mailBodiesFor("jobs-week-seeker@example.org")).hasSize(mailsBefore + 1);
    }
}
