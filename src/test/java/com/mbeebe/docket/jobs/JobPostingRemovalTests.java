package com.mbeebe.docket.jobs;

import com.mbeebe.docket.moderation.TargetKind;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3 rung 1 on the board. Removal and §6.4's automatic close are two
 * different facts and are kept apart: a closed posting is a finished one and still
 * renders, saying so; a removed one was never legitimate and stops rendering.
 *
 * <p>The Postgres container and GreenMail are shared across the whole run, so
 * every email here is prefixed "rm-jobs-", every company name is suite-unique and
 * every company gets its own mail domain — a shared one would auto-merge them.
 */
class JobPostingRemovalTests extends JobsTestBase {

    @Autowired
    JobPostingReportable removals;

    @Autowired
    JobSearchMailer digests;

    private void remove(long postingId) {
        clock.advance(Duration.ofMinutes(1));
        assertThat(removals.remove(TargetKind.JOB_POSTING, postingId, clock.instant()))
                .isTrue();
    }

    private String boardSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/jobs").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aRemovedPostingLeavesTheBoardItsOwnPageAndTheCompanyPage() throws Exception {
        Cookie poster = posterAt("rm-jobs-p1@example.org", "Polly Poster",
                "Kestrel Cranes RM1", "kestrelcranes-rm1.example");
        long companyId = companies.named("Kestrel Cranes RM1").id();
        long postingId = postJob(poster, companyId, "Crane Operator Reported");
        Cookie seeker = completeMember("rm-jobs-s1@example.org", "Seeka Seeker");

        assertThat(boardSeenBy(seeker)).contains("Crane Operator Reported");
        mvc.perform(get("/companies/" + companyId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        remove(postingId);

        assertThat(boardSeenBy(seeker)).doesNotContain("Crane Operator Reported");
        mvc.perform(get("/jobs/" + postingId).cookie(seeker))
                .andExpect(status().isNotFound());
        // Its poster gets the same 404: removal is total, not a private half-state.
        mvc.perform(get("/jobs/" + postingId).cookie(poster))
                .andExpect(status().isNotFound());
        assertThat(mvc.perform(get("/companies/" + companyId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .doesNotContain("Crane Operator Reported");
        // Logged out is a surface like any other (§8.4).
        assertThat(mvc.perform(get("/jobs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .doesNotContain("Crane Operator Reported");
    }

    @Test
    void aRemovedPostingIsNotTheSameThingAsAClosedOne() throws Exception {
        Cookie poster = posterAt("rm-jobs-p2@example.org", "Closa Poster",
                "Harbour Freight RM2", "harbourfreight-rm2.example");
        long companyId = companies.named("Harbour Freight RM2").id();
        long closedId = postJob(poster, companyId, "Finished Role RM2");
        long removedId = postJob(poster, companyId, "Illegitimate Role RM2");
        Cookie seeker = completeMember("rm-jobs-s2@example.org", "Closa Seeker");

        // The window's edge closes one of them; the ladder removes the other.
        clock.advance(JobService.WINDOW.plusDays(1));
        sweepClosedPostings();
        remove(removedId);

        // A closed posting still has a page and still tells the truth about itself.
        assertThat(mvc.perform(get("/jobs/" + closedId).cookie(seeker))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("Finished Role RM2");
        // A removed one has no page at all.
        mvc.perform(get("/jobs/" + removedId).cookie(seeker))
                .andExpect(status().isNotFound());
        // Both are off the board, but for different reasons and by different facts.
        String board = boardSeenBy(seeker);
        assertThat(board).doesNotContain("Finished Role RM2");
        assertThat(board).doesNotContain("Illegitimate Role RM2");
        // A closed posting is still reportable — the Report about its salary range
        // does not expire with the window — while a removed one is not.
        assertThat(removals.visibleToReporter(TargetKind.JOB_POSTING, closedId,
                Optional.empty())).isPresent();
        assertThat(removals.visibleToReporter(TargetKind.JOB_POSTING, removedId,
                Optional.empty())).isEmpty();
    }

    @Test
    void aRemovedPostingLeavesTheFeedRailAndTheCardOnAJobAttachedPost() throws Exception {
        Cookie poster = posterAt("rm-jobs-p3@example.org", "Raila Poster",
                "Northwind Optics RM3", "northwindoptics-rm3.example");
        long companyId = companies.named("Northwind Optics RM3").id();
        long postingId = postJob(poster, companyId, "Lens Grinder RM3");
        Cookie sharer = completeMember("rm-jobs-s3@example.org", "Raila Sharer");
        connect(sharer, poster);
        mvc.perform(post("/jobs/" + postingId + "/share").cookie(sharer)
                        .param("body", "Worth a look, this one."))
                .andExpect(status().is3xxRedirection());

        String before = mvc.perform(get("/").cookie(sharer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(before).contains("Lens Grinder RM3");

        remove(postingId);

        // §2.3's rail and §5.2.2's attached card both derive from the board, so
        // both stop naming it — the card simply is not rendered.
        String after = mvc.perform(get("/").cookie(sharer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(after).doesNotContain("Lens Grinder RM3");
        // The member's own words survive: removing the posting is not removing them.
        assertThat(mvc.perform(get("/p/" + memberId(sharer)).cookie(sharer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("Worth a look, this one.");
    }

    @Test
    void aRemovedPostingIsAbsentFromSavedSearchResultsAndFromTheirDigest() throws Exception {
        Cookie poster = posterAt("rm-jobs-p4@example.org", "Digesta Poster",
                "Quarry Works RM4", "quarryworks-rm4.example");
        long companyId = companies.named("Quarry Works RM4").id();
        Cookie seeker = completeMember("rm-jobs-s4@example.org", "Digesta Seeker");
        mvc.perform(post("/jobs/searches").cookie(seeker)
                        .param("q", "Zephyrine").param("frequency", "DAILY"))
                .andExpect(status().is3xxRedirection());
        clock.advance(Duration.ofMinutes(1));
        long removedId = postJob(poster, companyId, "Zephyrine Quarrier RM4");
        clock.advance(Duration.ofMinutes(1));
        postJob(poster, companyId, "Zephyrine Stonecutter RM4");

        assertThat(boardSeenBy(seeker)).contains("Zephyrine Quarrier RM4");

        remove(removedId);

        // The board's keyword filter and the digest come through one selection, so
        // they cannot disagree about which postings exist.
        assertThat(mvc.perform(get("/jobs").cookie(seeker).param("q", "Zephyrine"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .doesNotContain("Zephyrine Quarrier RM4")
                .contains("Zephyrine Stonecutter RM4");
        clock.advance(Duration.ofDays(1));
        digests.runDue(clock.instant());
        // Mailbox-scoped, never a global count: this seeker's own digests only.
        String digest = String.join("\n", mailBodiesFor("rm-jobs-s4@example.org"));
        assertThat(digest).doesNotContain("Zephyrine Quarrier RM4");
        assertThat(digest).contains("Zephyrine Stonecutter RM4");
    }

    @Test
    void aRemovedPostingLeavesItsApplicantsListAndItsOwnQueue() throws Exception {
        Cookie poster = posterAt("rm-jobs-p5@example.org", "Queua Poster",
                "Salt Marsh Foods RM5", "saltmarshfoods-rm5.example");
        long companyId = companies.named("Salt Marsh Foods RM5").id();
        long postingId = postJob(poster, companyId, "Brine Taster RM5");
        Cookie applicant = completeMember("rm-jobs-a5@example.org", "Appla Applicant");
        mvc.perform(post("/jobs/" + postingId + "/apply").cookie(applicant)
                        .param("note", "")).andExpect(status().is3xxRedirection());

        assertThat(mvc.perform(get("/applications").cookie(applicant))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("Brine Taster RM5");

        remove(postingId);

        assertThat(mvc.perform(get("/applications").cookie(applicant))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .doesNotContain("Brine Taster RM5");
        mvc.perform(get("/jobs/" + postingId + "/applications").cookie(poster))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRestoredPostingIsBackOnTheBoardAndOnItsOwnPage() throws Exception {
        Cookie poster = posterAt("rm-jobs-p6@example.org", "Backa Poster",
                "Cedar Mill RM6", "cedarmill-rm6.example");
        long companyId = companies.named("Cedar Mill RM6").id();
        long postingId = postJob(poster, companyId, "Sawyer RM6");
        Cookie seeker = completeMember("rm-jobs-s6@example.org", "Backa Seeker");

        remove(postingId);
        assertThat(boardSeenBy(seeker)).doesNotContain("Sawyer RM6");

        assertThat(removals.restore(TargetKind.JOB_POSTING, postingId)).isTrue();

        assertThat(boardSeenBy(seeker)).contains("Sawyer RM6");
        mvc.perform(get("/jobs/" + postingId).cookie(seeker)).andExpect(status().isOk());
        assertThat(mvc.perform(get("/companies/" + companyId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("Sawyer RM6");
    }

    @Test
    void theBoardAnswersForJobPostingsAndForNoOtherKind() throws Exception {
        Cookie poster = posterAt("rm-jobs-p7@example.org", "Ownkind Poster",
                "Beacon Glass RM7", "beaconglass-rm7.example");
        long companyId = companies.named("Beacon Glass RM7").id();
        long postingId = postJob(poster, companyId, "Glazier RM7");

        assertThat(removals.forModeration(TargetKind.JOB_POSTING, postingId))
                .get()
                .satisfies(item -> {
                    assertThat(item.authorId()).contains(memberId(poster));
                    assertThat(item.href()).isEqualTo("/jobs/" + postingId);
                    assertThat(item.summary()).contains("Glazier RM7")
                            .contains("Beacon Glass RM7")
                            .contains("Real work for real pay.");
                    assertThat(item.removed()).isFalse();
                });
        assertThat(removals.forModeration(TargetKind.POST, postingId)).isEmpty();
        assertThat(removals.forModeration(TargetKind.COMPANY, postingId)).isEmpty();
        assertThat(removals.remove(TargetKind.MESSAGE, postingId, clock.instant())).isFalse();
        assertThat(removals.restore(TargetKind.PROFILE, postingId)).isFalse();
        assertThat(removals.forModeration(TargetKind.JOB_POSTING, 987_654_321L)).isEmpty();
    }
}
