package com.mbeebe.docket.company;

import com.mbeebe.docket.jobs.JobsTestBase;
import com.mbeebe.docket.moderation.TargetKind;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3 rung 1 on Company surfaces — "joke/spam entities are cleaned up
 * reactively". A removed Company stops being a page, stops being offered by
 * autocomplete and by search, and stops naming and linking itself from the
 * Positions that claim it; the Positions themselves are the Members' own claims
 * and stay.
 *
 * <p>The Postgres container is shared across the whole run, so every company name
 * here is suite-unique, every company gets its own mail domain — a shared one
 * would auto-merge them (§6.1) — and every email is prefixed "rm-co-".
 */
class CompanyRemovalTests extends JobsTestBase {

    @Autowired
    CompanyReportable removals;

    private void remove(long companyId) {
        clock.advance(Duration.ofMinutes(1));
        assertThat(removals.remove(TargetKind.COMPANY, companyId, clock.instant())).isTrue();
    }

    private String pageAt(String path, Cookie session) throws Exception {
        return mvc.perform(get(path).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aRemovedCompanyIsA404WhereItsPageWasForEveryoneSignedInOrOut() throws Exception {
        Cookie member = employeeAt("rm-co-e1@example.org", "Emma Employee",
                "Sockpuppet Holdings RM1");
        long companyId = companies.named("Sockpuppet Holdings RM1").id();

        assertThat(pageAt("/companies/" + companyId, member))
                .contains("Sockpuppet Holdings RM1");

        remove(companyId);

        mvc.perform(get("/companies/" + companyId).cookie(member))
                .andExpect(status().isNotFound());
        mvc.perform(get("/companies/" + companyId)).andExpect(status().isNotFound());
        // Every one of the page's doors, not just the front one.
        mvc.perform(get("/companies/" + companyId + "/edit").cookie(member))
                .andExpect(status().isNotFound());
        mvc.perform(get("/companies/" + companyId + "/history").cookie(member))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRemovedCompanyIsNeverOfferedByAutocompleteOrFoundBySearch() throws Exception {
        Cookie member = employeeAt("rm-co-e2@example.org", "Otto Autocomplete",
                "Zibbleflax Refineries RM2");
        long companyId = companies.named("Zibbleflax Refineries RM2").id();

        assertThat(pageAt("/companies/options?q=Zibbleflax", member))
                .contains("Zibbleflax Refineries RM2");
        assertThat(pageAt("/search?q=Zibbleflax", member))
                .contains("Zibbleflax Refineries RM2");

        remove(companyId);

        assertThat(pageAt("/companies/options?q=Zibbleflax", member))
                .doesNotContain("Zibbleflax Refineries RM2");
        assertThat(pageAt("/search?q=Zibbleflax", member))
                .doesNotContain("Zibbleflax Refineries RM2");
    }

    @Test
    void aRemovedCompanyStopsNamingAndLinkingItselfFromAPosition() throws Exception {
        Cookie member = employeeAt("rm-co-e3@example.org", "Posy Positionholder",
                "Grimwald Tanneries RM3");
        long companyId = companies.named("Grimwald Tanneries RM3").id();
        long memberId = memberId(member);

        String before = pageAt("/p/" + memberId, member);
        assertThat(before).contains("Grimwald Tanneries RM3");
        assertThat(before).contains("/companies/" + companyId);

        remove(companyId);

        String after = pageAt("/p/" + memberId, member);
        assertThat(after).doesNotContain("Grimwald Tanneries RM3");
        assertThat(after).doesNotContain("/companies/" + companyId);
        // The Position itself is the Member's own claim and is untouched.
        assertThat(after).contains("A role");
        assertThat(pageAt("/profile/edit", member)).contains("A role");
    }

    @Test
    void aRestoredCompanyIsBackAsAPageInAutocompleteAndOnItsPeoplesPositions()
            throws Exception {
        Cookie member = employeeAt("rm-co-e5@example.org", "Backa Employee",
                "Wobblegate Mills RM5");
        long companyId = companies.named("Wobblegate Mills RM5").id();
        long memberId = memberId(member);

        remove(companyId);
        mvc.perform(get("/companies/" + companyId).cookie(member))
                .andExpect(status().isNotFound());

        assertThat(removals.restore(TargetKind.COMPANY, companyId)).isTrue();

        assertThat(pageAt("/companies/" + companyId, member)).contains("Wobblegate Mills RM5");
        assertThat(pageAt("/companies/options?q=Wobblegate", member))
                .contains("Wobblegate Mills RM5");
        assertThat(pageAt("/p/" + memberId, member)).contains("Wobblegate Mills RM5");
    }

    @Test
    void aCompanyHasNoAuthorBecauseNobodySpeaksForIt() throws Exception {
        employeeAt("rm-co-e6@example.org", "Nobody Speaksforit", "Anonymous Anvils RM6");
        long companyId = companies.named("Anonymous Anvils RM6").id();

        assertThat(removals.forModeration(TargetKind.COMPANY, companyId))
                .get()
                .satisfies(item -> {
                    // The ladder's member-facing rungs simply do not apply here.
                    assertThat(item.authorId()).isEmpty();
                    assertThat(item.href()).isEqualTo("/companies/" + companyId);
                    assertThat(item.summary()).contains("Anonymous Anvils RM6");
                    assertThat(item.removed()).isFalse();
                });
    }

    @Test
    void theCompanyModuleAnswersForCompaniesAndForNoOtherKind() throws Exception {
        employeeAt("rm-co-e7@example.org", "Ownkind Employee", "Onlyours Ltd RM7");
        long companyId = companies.named("Onlyours Ltd RM7").id();

        assertThat(removals.visibleToReporter(TargetKind.COMPANY, companyId,
                Optional.empty())).isPresent();
        assertThat(removals.forModeration(TargetKind.POST, companyId)).isEmpty();
        assertThat(removals.forModeration(TargetKind.PROFILE, companyId)).isEmpty();
        assertThat(removals.remove(TargetKind.JOB_POSTING, companyId, clock.instant()))
                .isFalse();
        assertThat(removals.restore(TargetKind.MESSAGE, companyId)).isFalse();
        assertThat(removals.forModeration(TargetKind.COMPANY, 987_654_321L)).isEmpty();
    }
}
