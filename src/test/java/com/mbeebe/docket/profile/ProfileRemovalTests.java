package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.jobs.JobsTestBase;
import com.mbeebe.docket.moderation.TargetKind;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3 rung 1 on a Profile. Removing the page a Member publishes is not
 * ending the Member (that is the ladder's fourth rung): they still sign in and
 * still have their edit page, which is what makes §10.3's "the member is told
 * which state they are in" and §10.5's reversibility possible at all.
 *
 * <p>The load-bearing case is {@code pageForApplication}: §6.3's consented view
 * deliberately bypasses the Dial and both floors, so it is the one surface a
 * removed Profile could still have rendered on, and it has its own check.
 *
 * <p>The Postgres container and GreenMail are shared across the whole run, so
 * every email here is prefixed "rm-prof-" and every company name is suite-unique.
 */
class ProfileRemovalTests extends JobsTestBase {

    @Autowired
    ProfileReportable removals;

    @Autowired
    Members members;

    private void remove(long memberId) {
        clock.advance(Duration.ofMinutes(1));
        assertThat(removals.remove(TargetKind.PROFILE, memberId, clock.instant())).isTrue();
    }

    private String pageAt(String path, Cookie session) throws Exception {
        return mvc.perform(get(path).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aRemovedProfileIsA404ForStrangersConnectionsAndItsOwnerAlike() throws Exception {
        Cookie owner = completeMember("rm-prof-o1@example.org", "Ownie Owner");
        Cookie friend = completeMember("rm-prof-f1@example.org", "Frankie Friend");
        connect(owner, friend);
        long ownerId = memberId(owner);

        assertThat(pageAt("/p/" + ownerId, friend)).contains("Ownie Owner");

        remove(ownerId);

        mvc.perform(get("/p/" + ownerId).cookie(friend)).andExpect(status().isNotFound());
        mvc.perform(get("/p/" + ownerId)).andExpect(status().isNotFound());
        // No self-exception, unlike the §7.3 Block check right above it.
        mvc.perform(get("/p/" + ownerId).cookie(owner)).andExpect(status().isNotFound());
        // The Member is untouched: they still sign in and still have their edit page,
        // which is where a statement of reasons and an Appeal can reach them.
        assertThat(pageAt("/profile/edit", owner)).contains("Ownie Owner");
    }

    @Test
    void aRemovedProfileLeavesPeopleSearchAndTheCompanyPagesPeopleList() throws Exception {
        Cookie owner = employeeAt("rm-prof-o2@example.org", "Quintilla Vandersnoot",
                "Marchbank Looms RM2");
        Cookie looker = completeMember("rm-prof-l2@example.org", "Lookie Looker");
        long ownerId = memberId(owner);
        long companyId = companies.named("Marchbank Looms RM2").id();

        assertThat(pageAt("/search?q=Quintilla", looker)).contains("Quintilla Vandersnoot");
        assertThat(pageAt("/companies/" + companyId, looker))
                .contains("Quintilla Vandersnoot");

        remove(ownerId);

        assertThat(pageAt("/search?q=Quintilla", looker))
                .doesNotContain("Quintilla Vandersnoot");
        // The people list flatMaps ProfileService.pageFor, so it inherits the answer
        // without restating the rule (§8.5: the Dial is honoured on every surface).
        assertThat(pageAt("/companies/" + companyId, looker))
                .doesNotContain("Quintilla Vandersnoot");
    }

    @Test
    void aRemovedProfileTakesItsAuthorsPostsWithItBecauseAPostRidesTheProfile()
            throws Exception {
        Cookie author = completeMember("rm-prof-a3@example.org", "Rida Author");
        Cookie reader = completeMember("rm-prof-r3@example.org", "Rida Reader");
        connect(author, reader);
        long authorId = memberId(author);
        clock.advance(Duration.ofMinutes(1));
        String redirect = mvc.perform(post("/posts").cookie(author)
                        .param("body", "Words riding a Profile that was removed."))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(pageAt(redirect, reader)).contains("Words riding a Profile that was removed.");

        remove(authorId);

        // §5.4: a Post is visible exactly when its author's page is, so this needs
        // no second rule in the feed module.
        mvc.perform(get(redirect).cookie(reader)).andExpect(status().isNotFound());
    }

    @Test
    void aRemovedProfileDoesNotRenderInSixThreesConsentedApplicationViewEither()
            throws Exception {
        Cookie poster = posterAt("rm-prof-p4@example.org", "Postie Poster",
                "Vellum Bindery RM4", "vellumbindery-rm4.example");
        long companyId = companies.named("Vellum Bindery RM4").id();
        long postingId = postJob(poster, companyId, "Bookbinder RM4");
        Cookie applicant = completeMember("rm-prof-a4@example.org", "Applia Applicant");
        long applicantId = memberId(applicant);
        mvc.perform(post("/jobs/" + postingId + "/apply").cookie(applicant).param("note", ""))
                .andExpect(status().is3xxRedirection());
        long applicationId = applicationIdOn(poster, postingId);
        String path = "/jobs/" + postingId + "/applications/" + applicationId + "/profile";

        assertThat(pageAt(path, poster)).contains("Applia Applicant");

        // The applicant turns their Dial all the way down: §6.3's bypass means the
        // poster still sees the full Profile, which is exactly why removal needs its
        // own check on this path.
        mvc.perform(post("/profile/dial").cookie(applicant).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());
        assertThat(pageAt(path, poster)).contains("Applia Applicant");

        remove(applicantId);

        mvc.perform(get(path).cookie(poster)).andExpect(status().isNotFound());
    }

    @Test
    void aRestoredProfileIsBackAsAPageInSearchAndOnTheCompanyPage() throws Exception {
        Cookie owner = employeeAt("rm-prof-o5@example.org", "Bracklewaite Fenn",
                "Tidewater Kilns RM5");
        Cookie looker = completeMember("rm-prof-l5@example.org", "Backa Looker");
        long ownerId = memberId(owner);
        long companyId = companies.named("Tidewater Kilns RM5").id();

        remove(ownerId);
        mvc.perform(get("/p/" + ownerId).cookie(looker)).andExpect(status().isNotFound());

        assertThat(removals.restore(TargetKind.PROFILE, ownerId)).isTrue();

        assertThat(pageAt("/p/" + ownerId, looker)).contains("Bracklewaite Fenn");
        assertThat(pageAt("/search?q=Bracklewaite", looker)).contains("Bracklewaite Fenn");
        assertThat(pageAt("/companies/" + companyId, looker)).contains("Bracklewaite Fenn");
    }

    @Test
    void aProfileOutOfItsDialIsNotReportableButTheQueueStillReadsIt() throws Exception {
        Cookie owner = completeMember("rm-prof-o6@example.org", "Dialla Owner");
        Cookie stranger = completeMember("rm-prof-s6@example.org", "Dialla Stranger");
        long ownerId = memberId(owner);
        mvc.perform(post("/profile/dial").cookie(owner).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        assertThat(removals.visibleToReporter(TargetKind.PROFILE, ownerId,
                members.find(memberId(stranger)))).isEmpty();
        assertThat(removals.forModeration(TargetKind.PROFILE, ownerId))
                .get()
                .satisfies(item -> {
                    assertThat(item.summary()).contains("Dialla Owner");
                    assertThat(item.href()).isEqualTo("/p/" + ownerId);
                    // A Profile's author is its subject: one person answerable for it.
                    assertThat(item.authorId()).contains(ownerId);
                    assertThat(item.removed()).isFalse();
                });
    }

    @Test
    void theProfileModuleAnswersForProfilesAndForNoOtherKind() throws Exception {
        Cookie owner = completeMember("rm-prof-o7@example.org", "Ownkind Owner");
        long ownerId = memberId(owner);

        assertThat(removals.visibleToReporter(TargetKind.PROFILE, ownerId, Optional.empty()))
                .isPresent();
        assertThat(removals.forModeration(TargetKind.POST, ownerId)).isEmpty();
        assertThat(removals.forModeration(TargetKind.COMPANY, ownerId)).isEmpty();
        assertThat(removals.remove(TargetKind.MESSAGE, ownerId, clock.instant())).isFalse();
        assertThat(removals.restore(TargetKind.JOB_POSTING, ownerId)).isFalse();
        // A Member who does not exist has no Profile to report, even for the queue.
        assertThat(removals.forModeration(TargetKind.PROFILE, 987_654_321L)).isEmpty();
    }

    /** The queue's own outcome forms carry the Application's id — the only place it renders. */
    private long applicationIdOn(Cookie poster, long postingId) throws Exception {
        String queue = pageAt("/jobs/" + postingId + "/applications", poster);
        Matcher matcher = Pattern
                .compile("/jobs/" + postingId + "/applications/(\\d+)/")
                .matcher(queue);
        if (!matcher.find()) {
            throw new AssertionError("No application on the queue for posting " + postingId);
        }
        return Long.parseLong(matcher.group(1));
    }
}
