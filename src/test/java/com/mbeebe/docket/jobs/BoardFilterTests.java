package com.mbeebe.docket.jobs;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.5: one list, newest first, and seeker-chosen filters — keyword,
 * location and remote policy, salary floor (a number in one currency), company,
 * and "roles where I know someone" (a fact about the graph, never a weight).
 * The board is shared across the suite run, so every assertion is scoped by a
 * marker word unique to this class.
 */
class BoardFilterTests extends JobsTestBase {

    private String board(String query, Cookie... session) throws Exception {
        var request = get("/jobs" + query);
        if (session.length > 0) {
            request = request.cookie(session[0]);
        }
        return mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void filtersNarrowTheOneListWithoutReordering() throws Exception {
        Cookie poster = posterAt("jobs-filter-poster@example.org", "Fi Poster",
                "Filterworks Ltd", "filterworks-fl.example");
        long company = companies.named("Filterworks Ltd").id();

        mvc.perform(post("/jobs").cookie(poster)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Zorilla Archivist").param("location", "Leeds")
                        .param("remotePolicy", "ON_SITE")
                        .param("salaryMin", "40000").param("salaryMax", "48000")
                        .param("currency", "GBP")
                        .param("description", "Shelving the zorilla papers."))
                .andExpect(status().is3xxRedirection());
        clock.advance(java.time.Duration.ofMinutes(1));
        mvc.perform(post("/jobs").cookie(poster)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Zorilla Engineer").param("location", "Glasgow")
                        .param("remotePolicy", "REMOTE")
                        .param("salaryMin", "55000").param("salaryMax", "70000")
                        .param("currency", "GBP")
                        .param("description", "Pipelines, mostly."))
                .andExpect(status().is3xxRedirection());
        clock.advance(java.time.Duration.ofMinutes(1));
        mvc.perform(post("/jobs").cookie(poster)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Zorilla Analyst").param("location", "Berlin")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", "60000").param("salaryMax", "80000")
                        .param("currency", "EUR")
                        .param("description", "Numbers about zorillas."))
                .andExpect(status().is3xxRedirection());

        // Newest first, no ranking: Analyst, then Engineer, then Archivist.
        String all = board("?q=Zorilla");
        assertThat(all.indexOf("Zorilla Analyst"))
                .isLessThan(all.indexOf("Zorilla Engineer"));
        assertThat(all.indexOf("Zorilla Engineer"))
                .isLessThan(all.indexOf("Zorilla Archivist"));

        // Keyword reaches the description too.
        assertThat(board("?q=shelving")).contains("Zorilla Archivist")
                .doesNotContain("Zorilla Engineer");

        // Location.
        assertThat(board("?q=Zorilla&location=glasgow")).contains("Zorilla Engineer")
                .doesNotContain("Zorilla Archivist").doesNotContain("Zorilla Analyst");

        // Remote policy.
        assertThat(board("?q=Zorilla&remote=REMOTE")).contains("Zorilla Engineer")
                .doesNotContain("Zorilla Archivist");

        // §6.5: the salary floor — what the mandatory salary makes possible. A
        // floor is a number in one currency: the EUR posting never matches a GBP
        // floor, whatever its numbers.
        String floored = board("?q=Zorilla&floor=50000&currency=GBP");
        assertThat(floored).contains("Zorilla Engineer");
        assertThat(floored).doesNotContain("Zorilla Archivist");
        assertThat(floored).doesNotContain("Zorilla Analyst");

        // Company.
        assertThat(board("?q=Zorilla&company=filterworks")).contains("Zorilla Analyst");
        assertThat(board("?q=Zorilla&company=elsewhere")).doesNotContain("Zorilla Analyst");
    }

    @Test
    void rolesWhereIKnowSomeoneIsAFactAboutTheGraph() throws Exception {
        Cookie poster = posterAt("jobs-known-poster@example.org", "Kay Poster",
                "Knownworks Ltd", "knownworks-fl.example");
        long company = companies.named("Knownworks Ltd").id();
        postJob(poster, company, "Wombat Registrar");

        Cookie other = posterAt("jobs-known-other@example.org", "Otto Other",
                "Otherworks Ltd", "otherworks-fl.example");
        postJob(other, companies.named("Otherworks Ltd").id(), "Wombat Cartographer");

        // The seeker knows someone at Knownworks — the poster, as it happens —
        // and nobody at Otherworks.
        Cookie seeker = completeMember("jobs-known-seeker@example.org", "Sia Seeker");
        connect(seeker, poster);

        String known = board("?q=Wombat&known=on", seeker);
        assertThat(known).contains("Wombat Registrar");
        assertThat(known).doesNotContain("Wombat Cartographer");

        // Unticked: the same one list as everyone else's.
        String all = board("?q=Wombat", seeker);
        assertThat(all).contains("Wombat Registrar");
        assertThat(all).contains("Wombat Cartographer");
    }
}
