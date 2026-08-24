package com.mbeebe.docket.jobs;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.4, the heart of the board: a posting runs a fixed window then
 * closes automatically; anything untouched becomes "closed without response"
 * and the applicant is told; a poster with an unresolved queue cannot open a
 * new posting — the obligation follows the person — and resolving lifts it.
 */
class ClosureTests extends JobsTestBase {

    @Autowired
    PostingCloser closer;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theWindowClosesAutomaticallyAndSilenceBecomesAnOutcome() throws Exception {
        Cookie poster = posterAt("jobs-close-poster@example.org", "Clo Poster",
                "Closureworks Ltd", "closureworks-cl.example");
        long posting = postJob(poster, companies.named("Closureworks Ltd").id(),
                "Lighthouse Keeper");

        Cookie touched = completeMember("jobs-close-touched@example.org", "Tia Touched");
        Cookie untouched = completeMember("jobs-close-untouched@example.org", "Uma Untouched");
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(touched))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(untouched))
                .andExpect(status().is3xxRedirection());

        long touchedApp = jdbc.queryForObject("""
                select a.id from application a join member m on m.id = a.applicant_id
                where a.posting_id = %d and m.email = 'jobs-close-touched@example.org'
                """.formatted(posting), Long.class);
        mvc.perform(post("/jobs/" + posting + "/applications/" + touchedApp + "/advance")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());

        // Not due yet: the sweep leaves this posting alone.
        closer.closeDue(clock.instant());
        assertThat(jdbc.queryForObject(
                "select closed_at is null from job_posting where id = " + posting,
                Boolean.class)).isTrue();

        // Past the window's edge: the sweep executes the guarantee.
        clock.advance(Duration.ofDays(31));
        closer.closeDue(clock.instant());

        // The posting is closed — a dated fact.
        assertThat(jdbc.queryForObject(
                "select closed_at is not null from job_posting where id = " + posting,
                Boolean.class)).isTrue();

        // The untouched Application became "closed without response" and the
        // applicant was told (§6.4, §6.5).
        assertThat(mvc.perform(get("/applications").cookie(untouched))
                .andReturn().getResponse().getContentAsString())
                .contains("Closed without response");
        assertThat(mailBodiesFor("jobs-close-untouched@example.org"))
                .anySatisfy(body -> assertThat(body).contains("closed without a response"));

        // The advanced Application was already resolved: untouched by the sweep,
        // and no closure mail for its applicant.
        assertThat(mvc.perform(get("/applications").cookie(touched))
                .andReturn().getResponse().getContentAsString()).contains("Advanced");
        assertThat(mailBodiesFor("jobs-close-touched@example.org"))
                .noneSatisfy(body -> assertThat(body).contains("closed without a response"));

        // The board no longer lists it; the page says closed; applying is refused.
        assertThat(mvc.perform(get("/jobs")).andReturn().getResponse().getContentAsString())
                .doesNotContain("Lighthouse Keeper");
        assertThat(mvc.perform(get("/jobs/" + posting)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("This posting has closed.");
        Cookie late = completeMember("jobs-close-late@example.org", "Lars Late");
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(late))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anUnresolvedQueueBlocksTheNextPostingUntilResolved() throws Exception {
        Cookie poster = posterAt("jobs-block-poster@example.org", "Bea Blocked",
                "Blockworks Ltd", "blockworks-cl.example");
        long company = companies.named("Blockworks Ltd").id();
        long neglected = postJob(poster, company, "Neglected Role");

        Cookie applicant = completeMember("jobs-block-applicant@example.org", "Ann Applicant");
        mvc.perform(post("/jobs/" + neglected + "/apply").cookie(applicant))
                .andExpect(status().is3xxRedirection());

        clock.advance(Duration.ofDays(31));
        closer.closeDue(clock.instant());

        // §6.4: the queue is unresolved — a new posting is refused, and the
        // refusal names the posting the poster can act on.
        var refusal = mvc.perform(post("/jobs").cookie(poster)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Next Role").param("location", "Leeds")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", "45000").param("salaryMax", "60000")
                        .param("currency", "GBP").param("description", "Words."))
                .andExpect(status().isForbidden())
                .andReturn().getResponse();
        assertThat(refusal.getErrorMessage()).contains("Neglected Role");

        // The form page reports the same block (§3.2's shape).
        assertThat(mvc.perform(get("/jobs/new").cookie(poster))
                .andReturn().getResponse().getContentAsString()).contains("Neglected Role");

        // Resolving the silenced Application lifts the block — late, but owed.
        long applicationId = jdbc.queryForObject(
                "select id from application where posting_id = " + neglected, Long.class);
        mvc.perform(post("/jobs/" + neglected + "/applications/" + applicationId + "/not-select")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());
        assertThat(mvc.perform(get("/applications").cookie(applicant))
                .andReturn().getResponse().getContentAsString()).contains("Not selected");

        mvc.perform(post("/jobs").cookie(poster)
                        .param("companyId", String.valueOf(company))
                        .param("title", "Next Role").param("location", "Leeds")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", "45000").param("salaryMax", "60000")
                        .param("currency", "GBP").param("description", "Words."))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void theEmptyBoardSaysWhatItIsInTheSpecsOwnWords() throws Exception {
        // Sweep everything any suite left open: past every window, the board is
        // honestly empty — §13.4's copy is spec surface, verbatim.
        clock.advance(Duration.ofDays(31));
        closer.closeDue(clock.instant());
        String board = mvc.perform(get("/jobs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(board).contains("No open postings right now.");
        assertThat(board).contains("there's no backlog you can't see.");
    }
}
