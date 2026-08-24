package com.mbeebe.docket.jobs;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.3: a posting is always authored by a person holding the whole gate —
 * Completeness (§3.2, POST_JOB) plus the §6.2 trust gate (current Position AND a
 * Work verification at the Company's domain) — and carries a mandatory real
 * salary range. §8.4: the board and every posting page are browsable logged out.
 */
class PostingTests extends JobsTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void postingIsGatedByCompletenessAndTheTrustGate() throws Exception {
        // A member with a current Position but NO work verification: the gate fails.
        Cookie unverified = employeeAt("jobs-post-unverified@example.org", "Una Verified",
                "Quayside Post Co");
        long company = companies.named("Quayside Post Co").id();
        mvc.perform(post("/jobs").cookie(unverified)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Engineer").param("location", "Leeds")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", "45000").param("salaryMax", "60000")
                        .param("currency", "GBP")
                        .param("description", "Words."))
                .andExpect(status().isForbidden());

        // A verified member whose Position there has ENDED: currency gates (§16).
        Cookie ended = posterAt("jobs-post-ended@example.org", "Enda Roll",
                "Quayside Post Co", "quayside-post.example");
        String editPage = mvc.perform(get("/profile/edit").cookie(ended))
                .andReturn().getResponse().getContentAsString();
        var m = java.util.regex.Pattern.compile("/profile/positions/(\\d+)/end").matcher(editPage);
        if (!m.find()) {
            throw new AssertionError("No end-position form");
        }
        mvc.perform(post("/profile/positions/" + m.group(1) + "/end").cookie(ended)
                        .param("endMonth", "2").param("endYear", "2021"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/jobs").cookie(ended)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Engineer").param("location", "Leeds")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", "45000").param("salaryMax", "60000")
                        .param("currency", "GBP")
                        .param("description", "Words."))
                .andExpect(status().isForbidden());

        // An INCOMPLETE member: §3.2's POST_JOB is not earned, whatever else holds.
        Cookie incomplete = signUpAndIn("jobs-post-incomplete@example.org");
        mvc.perform(post("/jobs").cookie(incomplete)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Engineer").param("location", "Leeds")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", "45000").param("salaryMax", "60000")
                        .param("currency", "GBP")
                        .param("description", "Words."))
                .andExpect(status().isForbidden());

        // Signed out: to the sign-in page.
        mvc.perform(post("/jobs").param("companyId", String.valueOf(company))
                        .param("title", "Engineer").param("location", "")
                        .param("remotePolicy", "REMOTE")
                        .param("salaryMin", "45000").param("salaryMax", "60000")
                        .param("currency", "GBP").param("description", "Words."))
                .andExpect(status().is3xxRedirection());

        // Nothing above landed a row.
        assertThat(jdbc.queryForObject(
                "select count(*) from job_posting where company_id = " + company, Long.class))
                .isZero();
    }

    @Test
    void theSalaryRangeIsMandatoryAndReal() throws Exception {
        Cookie poster = posterAt("jobs-post-salary@example.org", "Sally Range",
                "Rangeworks Jobs", "rangeworks-jobs.example");
        long company = companies.named("Rangeworks Jobs").id();

        record Bad(String min, String max, String currency) { }
        for (Bad bad : new Bad[] {
                new Bad("", "", "GBP"),            // missing entirely
                new Bad("50000", "", "GBP"),       // half a range
                new Bad("50000", "50000", "GBP"),  // a single number is not a range
                new Bad("60000", "50000", "GBP"),  // upside down
                new Bad("0", "50000", "GBP"),      // nothing is not a salary
                new Bad("50000", "60000", "ZZZ"),  // not a currency Docket knows
        }) {
            mvc.perform(post("/jobs").cookie(poster)
                            .param("companyId", String.valueOf(company))
                            .param("title", "Underpaid role").param("location", "Leeds")
                            .param("remotePolicy", "HYBRID")
                            .param("salaryMin", bad.min()).param("salaryMax", bad.max())
                            .param("currency", bad.currency())
                            .param("description", "Words."))
                    .andExpect(status().isUnprocessableEntity());
        }
        assertThat(jdbc.queryForObject(
                "select count(*) from job_posting where company_id = " + company, Long.class))
                .isZero();
    }

    @Test
    void aVerifiedCurrentEmployeePostsAndTheWholeWebCanRead() throws Exception {
        Cookie poster = posterAt("jobs-post-flow@example.org", "Petra Poster",
                "Harbourline Jobs", "harbourline-jobs.example");
        long company = companies.named("Harbourline Jobs").id();
        long posting = postJob(poster, company, "Marine Engineer");

        // §8.4: the board is browsable logged out; §6.3: salary in every list row.
        String board = mvc.perform(get("/jobs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(board).contains("Marine Engineer");
        assertThat(board).contains("Harbourline Jobs");
        assertThat(board).contains("£45,000–£60,000");

        // The posting page, logged out: salary at the top, the poster named and
        // linked — accountability is a person (§6.3) — and the Company linked.
        String page = mvc.perform(get("/jobs/" + posting)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("£45,000–£60,000");
        assertThat(page).contains("Petra Poster");
        assertThat(page).contains("/p/" + memberId(poster));
        assertThat(page).contains("/companies/" + company);
        assertThat(page).contains("Real work for real pay.");

        // The Company page's postings section now lists it.
        String companyPage = mvc.perform(get("/companies/" + company))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(companyPage).contains("Marine Engineer");
        assertThat(companyPage).contains("/jobs/" + posting);

        // The stored row is facts only: a window edge, no state flag.
        assertThat(jdbc.queryForObject("""
                select count(*) from job_posting
                where id = %d and closes_at = posted_at + interval '30 days'
                  and closed_at is null
                """.formatted(posting), Long.class)).isEqualTo(1);
    }
}
