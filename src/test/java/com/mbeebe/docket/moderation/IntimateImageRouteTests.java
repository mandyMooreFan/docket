package com.mbeebe.docket.moderation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.5 and OSA s.20A: the intimate image route. A distinct public form needing
 * no account, carrying the prescribed declarations, and a hide that happens on receipt
 * rather than on review — which is how the 48-hour duty in s.10(3A) is met
 * structurally rather than by somebody being awake.
 */
class IntimateImageRouteTests extends ModerationTestBase {

    private void submit(String locator, String contact, String... omit) throws Exception {
        List<String> omitted = List.of(omit);
        var request = post("/safety/intimate-image")
                .param("locator", locator)
                .param("contact", contact);
        if (!omitted.contains("subjectDeclared")) {
            request = request.param("subjectDeclared", "true");
        }
        if (!omitted.contains("goodFaith")) {
            request = request.param("goodFaith", "true");
        }
        mvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void theContentIsHiddenWhenTheReportArrivesAndNoClockIsEverRunning() throws Exception {
        Cookie author = completeMember("mod-iir-author@example.org", "Holding Author");
        Cookie reader = completeMember("mod-iir-reader@example.org", "Holding Reader");
        connect(author, reader);
        long postId = compose(author, "A post that gets held on receipt.");
        assertThat(feedSeenBy(reader)).contains("A post that gets held on receipt.");

        submit("http://localhost:8080/posts/" + postId, "depicted@example.org");

        // The hide is synchronous with the report. Not "within 48 hours" — now, in the
        // same request, before any person has looked at it.
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isNotFound());
        assertThat(feedSeenBy(reader)).doesNotContain("A post that gets held on receipt.");

        // The structural claim, which is the one worth testing: with nobody reviewing
        // anything, there is no moment at which the content comes back and no deadline
        // that can pass. Long past 48 hours, it is still gone.
        clock.advance(Duration.ofHours(60));
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isNotFound());
        clock.advance(Duration.ofDays(30));
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isNotFound());
    }

    @Test
    void theHoldIsDisclosedToBothParties() throws Exception {
        Cookie author = completeMember("mod-iir-disc-a@example.org", "Disclosed Author");
        Cookie reader = completeMember("mod-iir-disc-b@example.org", "Disclosed Reader");
        connect(author, reader);
        long postId = compose(author, "A post whose hold is disclosed.");

        submit("http://localhost:8080/posts/" + postId, "mod-iir-depicted@example.org");

        // §10.5: "disclosed to both parties" — and the uploader's copy must not read as
        // a finding against them, because the hold is not one.
        List<String> toUploader = mailBodiesFor("mod-iir-disc-a@example.org");
        assertThat(toUploader).isNotEmpty();
        assertThat(toUploader.getLast()).contains("hidden");
        assertThat(toUploader.getLast()).contains("not a finding against you");

        List<String> toReporter = mailBodiesFor("mod-iir-depicted@example.org");
        assertThat(toReporter).isNotEmpty();
        assertThat(toReporter.getLast()).contains("hidden now");
    }

    @Test
    void restoringPutsTheContentBackExactlyAsItWas() throws Exception {
        Cookie author = completeMember("mod-iir-rest-a@example.org", "Restored Author");
        Cookie reader = completeMember("mod-iir-rest-b@example.org", "Restoring Reader");
        connect(author, reader);
        long postId = compose(author, "A post that comes back after review.");

        submit("http://localhost:8080/posts/" + postId, "mod-iir-rest@example.org");
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isNotFound());

        Cookie ownerSession = owner();
        String queue = mvc.perform(get("/moderation/intimate-images").cookie(ownerSession))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(queue).contains("/posts/" + postId);
        long reportId = Long.parseLong(queue.replaceAll(
                "(?s).*?/moderation/intimate-images/(\\d+).*", "$1"));

        mvc.perform(post("/moderation/intimate-images/" + reportId).cookie(ownerSession)
                        .param("decision", "RESTORED")
                        .param("reason", "Not what it was reported to be."))
                .andExpect(status().is3xxRedirection());

        // Reversibility is what makes takedown-on-accusation bearable (§10.5).
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isOk());
        assertThat(feedSeenBy(reader)).contains("A post that comes back after review.");
    }

    @Test
    void aReportWithoutTheStatutoryDeclarationsIsRefused() throws Exception {
        Cookie author = completeMember("mod-iir-decl-a@example.org", "Declaring Author");
        Cookie reader = completeMember("mod-iir-decl-b@example.org", "Declaring Reader");
        connect(author, reader);
        long postId = compose(author, "A post nobody validly reported.");

        // s.20A(2) requires the good-faith statement and the subject-or-acting-for
        // declaration. Without them there is no report — and so no takedown.
        submit("http://localhost:8080/posts/" + postId, "someone@example.org", "goodFaith");
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isOk());

        submit("http://localhost:8080/posts/" + postId, "someone@example.org", "subjectDeclared");
        mvc.perform(get("/posts/" + postId).cookie(reader)).andExpect(status().isOk());
    }

    @Test
    void anImpreciseLocationIsAcceptedAndSaysHonestlyThatNothingIsHiddenYet() throws Exception {
        // §10.5 accepts an imprecise location, because a non-member cannot see a private
        // Thread. The product must not guess — taking down somebody else's post on a
        // guess would be a worse failure than saying it could not act yet.
        String page = mvc.perform(post("/safety/intimate-image")
                        .param("locator", "a photo of me somewhere in his messages")
                        .param("subjectDeclared", "true")
                        .param("goodFaith", "true")
                        .param("contact", "mod-iir-vague@example.org"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page).contains("could not work out");
        assertThat(page).contains("not going to guess");
    }

    @Test
    void theRouteNeedsNoAccountAndIsReachableLoggedOut() throws Exception {
        String form = mvc.perform(get("/safety/intimate-image"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(form).contains("you do not need an account");
        // A distinct route, not the general report flow (§10.5).
        assertThat(form).contains("not the ordinary report route");
    }

    @Test
    void reportsAreRateLimitedPerAddress() throws Exception {
        for (int i = 0; i < IntimateImageService.MAX_PER_ADDRESS_PER_HOUR; i++) {
            submit("nothing precise " + i, "mod-iir-flood@example.org");
        }
        String page = mvc.perform(post("/safety/intimate-image")
                        .param("locator", "one more")
                        .param("subjectDeclared", "true")
                        .param("goodFaith", "true")
                        .param("contact", "mod-iir-flood@example.org"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page).contains("Too many reports");
        // Rate-limited is not a dead end for someone in real trouble.
        assertThat(page).contains("safety page");
    }
}
