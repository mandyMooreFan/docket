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
 * SPEC.md §6.3: the Profile IS the Application — one click plus an optional
 * note; applying grants the poster a full view of the Profile for that
 * Application whatever the Dial, plus Mutuals. §6.4: Outcomes are dated facts
 * the applicant can always see. §9.2: under-18s apply normally.
 */
class ApplicationTests extends JobsTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void applyingIsOneClickPlusANoteAndTheReceiptIsMailed() throws Exception {
        Cookie poster = posterAt("jobs-app-poster@example.org", "Quinn Poster",
                "Applyworks Ltd", "applyworks-app.example");
        long company = companies.named("Applyworks Ltd").id();
        long posting = postJob(poster, company, "Toolsmith");

        // Signed out: applying takes a profile.
        mvc.perform(post("/jobs/" + posting + "/apply"))
                .andExpect(status().is3xxRedirection());

        // Incomplete: the apply button reports the §3.2 check — a 403, with why.
        Cookie incomplete = signUpAndIn("jobs-app-incomplete@example.org");
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(incomplete))
                .andExpect(status().isForbidden());

        // Your own posting is not something you apply to.
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(poster))
                .andExpect(status().isForbidden());

        // One click plus an optional note.
        Cookie applicant = completeMember("jobs-app-applicant@example.org", "Ada Applicant");
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(applicant)
                        .param("note", "I build tools."))
                .andExpect(status().is3xxRedirection());

        // The transactional receipt (§6.5), to the applicant's own mailbox.
        var bodies = mailBodiesFor("jobs-app-applicant@example.org");
        assertThat(bodies).anySatisfy(body -> {
            assertThat(body).contains("Toolsmith");
            assertThat(body).contains("received");
        });

        // The applicant can always see their Application's state (§6.4).
        String mine = mvc.perform(get("/applications").cookie(applicant))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(mine).contains("Toolsmith");
        assertThat(mine).contains("Received");

        // Re-applying to the same posting is refused; the one row stands.
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(applicant))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("""
                select count(*) from application a join member m on m.id = a.applicant_id
                where a.posting_id = %d and m.email = 'jobs-app-applicant@example.org'
                """.formatted(posting), Long.class)).isEqualTo(1);
    }

    @Test
    void anUnderEighteenAppliesLikeAnyoneElse() throws Exception {
        Cookie poster = posterAt("jobs-app-minorpost@example.org", "Mel Poster",
                "Minorwelcome Ltd", "minorwelcome-app.example");
        long posting = postJob(poster, companies.named("Minorwelcome Ltd").id(), "Apprentice");

        Cookie minor = completeMinor("jobs-app-minor@example.org", "Min Applicant");
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(minor)
                        .param("note", "Keen to start."))
                .andExpect(status().is3xxRedirection());

        // §9.2: the poster's queue shows them normally — profile, name, note.
        String queue = mvc.perform(get("/jobs/" + posting + "/applications").cookie(poster))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(queue).contains("Min Applicant");
        assertThat(queue).contains("Keen to start.");
    }

    @Test
    void applyingHandsThePosterTheFullProfileForThatApplicationOnly() throws Exception {
        Cookie poster = posterAt("jobs-app-queue@example.org", "Petra Queue",
                "Queueworks Ltd", "queueworks-app.example");
        long posting = postJob(poster, companies.named("Queueworks Ltd").id(), "Archivist");

        // The applicant's Dial is CONNECTIONS_ONLY, and they are not connected
        // to the poster — outside the Application, their page does not exist.
        Cookie applicant = completeMember("jobs-app-dialled@example.org", "Dahlia Dialled");
        long applicantId = memberId(applicant);
        mvc.perform(post("/profile/dial").cookie(applicant).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        // A mutual: connected to both poster and applicant (§6.3 — how a
        // referral has always worked).
        Cookie mutual = completeMember("jobs-app-mutual@example.org", "Mo Mutual");
        connect(mutual, poster);
        connect(mutual, applicant);

        mvc.perform(post("/jobs/" + posting + "/apply").cookie(applicant)
                        .param("note", "Order from chaos."))
                .andExpect(status().is3xxRedirection());

        // The ordinary Profile page honours the Dial: to the poster, a 404.
        mvc.perform(get("/p/" + applicantId).cookie(poster))
                .andExpect(status().isNotFound());

        // The queue: the applicant's card, their note, and the Mutuals.
        String queue = mvc.perform(get("/jobs/" + posting + "/applications").cookie(poster))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(queue).contains("Dahlia Dialled");
        assertThat(queue).contains("Order from chaos.");
        assertThat(queue).contains("Mo Mutual");

        // The application-scoped view: the full Profile, whatever the Dial (§6.3).
        long applicationId = jdbc.queryForObject(
                "select id from application where posting_id = " + posting
                        + " and applicant_id = " + applicantId, Long.class);
        String profile = mvc.perform(get("/jobs/" + posting + "/applications/"
                        + applicationId + "/profile").cookie(poster))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(profile).contains("Dahlia Dialled");
        assertThat(profile).contains("A headline");

        // Scoped to the poster: the applicant's Dial holds against everyone else.
        Cookie stranger = completeMember("jobs-app-stranger@example.org", "Stan Stranger");
        mvc.perform(get("/jobs/" + posting + "/applications").cookie(stranger))
                .andExpect(status().isNotFound());
        mvc.perform(get("/jobs/" + posting + "/applications/" + applicationId + "/profile")
                        .cookie(stranger))
                .andExpect(status().isNotFound());
        mvc.perform(get("/jobs/" + posting + "/applications"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void outcomesAreDatedFactsTheApplicantAlwaysSees() throws Exception {
        Cookie poster = posterAt("jobs-app-outcome@example.org", "Olive Poster",
                "Outcomeworks Ltd", "outcomeworks-app.example");
        long posting = postJob(poster, companies.named("Outcomeworks Ltd").id(), "Cooper");

        Cookie advanced = completeMember("jobs-app-advanced@example.org", "Adele Advanced");
        Cookie declined = completeMember("jobs-app-declined@example.org", "Dec Lined");
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(advanced))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(declined))
                .andExpect(status().is3xxRedirection());

        long advancedApp = jdbc.queryForObject("""
                select a.id from application a join member m on m.id = a.applicant_id
                where a.posting_id = %d and m.email = 'jobs-app-advanced@example.org'
                """.formatted(posting), Long.class);
        long declinedApp = jdbc.queryForObject("""
                select a.id from application a join member m on m.id = a.applicant_id
                where a.posting_id = %d and m.email = 'jobs-app-declined@example.org'
                """.formatted(posting), Long.class);

        // Only the poster resolves a queue.
        Cookie stranger = completeMember("jobs-app-nosey@example.org", "Nosey Parker");
        mvc.perform(post("/jobs/" + posting + "/applications/" + advancedApp + "/advance")
                        .cookie(stranger))
                .andExpect(status().isNotFound());

        mvc.perform(post("/jobs/" + posting + "/applications/" + advancedApp + "/advance")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/jobs/" + posting + "/applications/" + declinedApp + "/not-select")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());

        // Each applicant sees their own state (§6.4).
        assertThat(mvc.perform(get("/applications").cookie(advanced))
                .andReturn().getResponse().getContentAsString()).contains("Advanced");
        assertThat(mvc.perform(get("/applications").cookie(declined))
                .andReturn().getResponse().getContentAsString()).contains("Not selected");

        // Not selected closes the application, and the applicant is told by mail;
        // advanced is not a closure — the poster's own reply is messaging's (#36).
        assertThat(mailBodiesFor("jobs-app-declined@example.org"))
                .anySatisfy(body -> assertThat(body).contains("was not selected"));
        assertThat(mailBodiesFor("jobs-app-advanced@example.org"))
                .noneSatisfy(body -> assertThat(body).contains("was not selected"));

        // An Outcome is a dated fact: set once, never rewritten.
        mvc.perform(post("/jobs/" + posting + "/applications/" + advancedApp + "/not-select")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject(
                "select outcome from application where id = " + advancedApp, String.class))
                .isEqualTo("ADVANCED");
    }
}
