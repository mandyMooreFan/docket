package com.mbeebe.docket.moderation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §15.3 and §11.3: the statutory surfaces the product itself must carry — the
 * conduct policy, the honest statement of who reviews, the transparency log, the
 * data-protection complaints route, and the persistent affordance that leads to them.
 */
class StatutorySurfaceTests extends ModerationTestBase {

    @Test
    void everyPageCarriesTheSafetyAffordance() throws Exception {
        Cookie member = completeMember("mod-stat-nav@example.org", "Navigating Member");

        // Children's code Standard 15: prominence is placement, not new capability. It
        // has to be on every page, logged in and out, like the Source link beside it.
        for (String page : List.of("/", "/jobs", "/network")) {
            assertThat(mvc.perform(get(page).cookie(member))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString())
                    .contains("/safety");
        }
        assertThat(mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).contains("/safety");
    }

    @Test
    void theSafetyPageStatesTheRealResponseExpectationRatherThanImplyingADesk() throws Exception {
        String safety = mvc.perform(get("/safety"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // §10.1 requires this said in plain words, and requires it not be dressed up.
        assertThat(safety).contains("One person reviews reports");
        assertThat(safety).contains("one timezone");
        assertThat(safety).doesNotContainIgnoringCase("our team");
        assertThat(safety).doesNotContainIgnoringCase("24/7");
        assertThat(safety).doesNotContainIgnoringCase("service level");
    }

    @Test
    void theConductPolicyEnumeratesSixThingsAndSaysEverythingElseIsNotAnOffence() throws Exception {
        String conduct = mvc.perform(get("/conduct"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(conduct).contains("Everything not on it is not an offence");
        for (ReportCategory category : ReportCategory.values()) {
            assertThat(conduct).contains(category.label());
        }
        // §9.5: "for all users" is load-bearing — it is what disapplies s.12(5)'s
        // mandatory age assurance, and narrowing it to minors would reopen §9 silently.
        assertThat(conduct).contains("applies to everyone here, not only to members under 18");
    }

    @Test
    void theTransparencyLogCountsByCategoryAndNamesNobody() throws Exception {
        Cookie author = completeMember("mod-stat-log-a@example.org", "Logged Author");
        Cookie reporter = completeMember("mod-stat-log-b@example.org", "Logged Reporter");
        connect(author, reporter);
        long postId = compose(author, "A post that ends up in the counts.");
        report(reporter, "POST", postId, "SPAM", "This is an advert.");
        Cookie ownerSession = owner();
        act(ownerSession, oldestOpenReportId(ownerSession), "dismiss",
                "reason", "It is not an advert.");

        // Public: a log only the moderated can read is not transparency.
        String log = mvc.perform(get("/transparency"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(log).contains("Bulk or commercial spam");
        // No names, either side of the report.
        assertThat(log).doesNotContain("Logged Author");
        assertThat(log).doesNotContain("Logged Reporter");
        assertThat(log).doesNotContain("mod-stat-log-a@example.org");
        assertThat(log).doesNotContain("This is an advert.");
        // Dismissals are published beside upholdings (§10.3).
        assertThat(log).contains("Dismissed");
    }

    @Test
    void theTransparencyLogHasNoRowForReachReductionBecauseThereIsNone() throws Exception {
        String log = mvc.perform(get("/transparency"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // §10.3 refuses visibility limiting and shadowbanning outright. The log saying
        // so is the cheapest place to notice if that ever stopped being true.
        assertThat(log).doesNotContainIgnoringCase("demoted");
        assertThat(log).doesNotContainIgnoringCase("reach reduced");
        assertThat(log).contains("does not do either");
    }

    @Test
    void theDataProtectionRouteIsItsOwnFormAndAcknowledgesOnReceipt() throws Exception {
        String form = mvc.perform(get("/data-protection"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // §11.3 and §15.3: distinct from the moderation report flow and from §10.5's.
        assertThat(form).contains("not the route for reporting a post");
        assertThat(form).contains("You do not need an account");

        mvc.perform(post("/data-protection")
                        .param("contact", "mod-stat-dp@example.org")
                        .param("account", "You are still holding my email after I left."))
                .andExpect(status().isOk());

        List<String> mail = mailBodiesFor("mod-stat-dp@example.org");
        assertThat(mail).isNotEmpty();
        // Acknowledged now, not on the thirtieth day — the duty cannot lapse if it is
        // discharged in the same transaction.
        assertThat(mail.getLast()).contains("30 days");
        assertThat(mail.getLast()).contains("sent now rather than on the thirtieth day");
        // The ICO route is offered rather than hidden.
        assertThat(mail.getLast()).contains("ico.org.uk");
    }

    @Test
    void aDecisionCarriesTheStatementOfReasonsFields() throws Exception {
        Cookie author = completeMember("mod-stat-sor-a@example.org", "Reasoned Author");
        Cookie reporter = completeMember("mod-stat-sor-b@example.org", "Reasoned Reporter");
        connect(author, reporter);
        long postId = compose(author, "A post that earns a statement of reasons.");
        report(reporter, "POST", postId, "ILLEGAL_CONTENT", "This looks unlawful to me.");
        Cookie ownerSession = owner();

        act(ownerSession, oldestOpenReportId(ownerSession), "remove",
                "reason", "Unlawful under the Act.");

        List<String> toAuthor = mailBodiesFor("mod-stat-sor-a@example.org");
        assertThat(toAuthor).isNotEmpty();
        String statement = toAuthor.getLast();
        // DSA Art. 17's five contents, as far as they apply here (§15.4: v1 does not
        // target the EU, so they are carried rather than owed).
        assertThat(statement).contains("removed");                       // what was done
        assertThat(statement).contains("Unlawful under the Act.");       // the facts
        assertThat(statement).contains("a report from another member");  // report vs own-initiative
        assertThat(statement).contains("No automated system made this decision"); // automation
        assertThat(statement).contains("member conduct policy");         // the ground
    }
}
