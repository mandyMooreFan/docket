package com.mbeebe.docket.moderation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3: one Appeal, "described as what it is — the same person reconsidering
 * with new information". Upholding reverses the rung without erasing that it happened,
 * which is what keeps the transparency log truthful.
 */
class AppealTests extends ModerationTestBase {

    private long withdrawPostingFrom(Cookie author, Cookie reporter) throws Exception {
        connect(author, reporter);
        long postId = compose(author, "A post that draws a withdrawal.");
        report(reporter, "POST", postId, "SPAM", "Adverts, over and over.");
        Cookie ownerSession = owner();
        act(ownerSession, reportIdForPost(ownerSession, postId), "withdraw",
                "capability", "POST", "reason", "Repeated advertising.");
        return appealableActionId(author);
    }

    @Test
    void thereIsExactlyOneAppealAndTheSecondIsRefused() throws Exception {
        Cookie author = completeMember("mod-app-one-a@example.org", "Appealing Author");
        Cookie reporter = completeMember("mod-app-one-b@example.org", "Appealing Reporter");
        long actionId = withdrawPostingFrom(author, reporter);

        appeal(author, actionId, "The posts were about my own work.");

        String second = mvc.perform(post("/appeals/" + actionId).cookie(author)
                        .param("account", "Let me try that again."))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        assertThat(flat(second)).contains("already appealed");
        assertThat(flat(second)).containsIgnoringCase("there is one appeal");
    }

    @Test
    void theAppealPageSaysWhatAnAppealActuallyIs() throws Exception {
        Cookie author = completeMember("mod-app-hon-a@example.org", "Honest Author");
        Cookie reporter = completeMember("mod-app-hon-b@example.org", "Honest Reporter");
        long actionId = withdrawPostingFrom(author, reporter);

        String page = flat(mvc.perform(get("/appeals/" + actionId).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // §10.3 requires the honesty, and the product must not imply a second opinion
        // it does not have.
        assertThat(page).contains("the same person who made the decision");
        assertThat(page).contains("does not have an independent panel");
        assertThat(page).doesNotContainIgnoringCase("independent review");
    }

    @Test
    void anUpheldAppealReversesTheRungAndGivesTheCapabilityBack() throws Exception {
        Cookie author = completeMember("mod-app-up-a@example.org", "Reversed Author");
        Cookie reporter = completeMember("mod-app-up-b@example.org", "Reversed Reporter");
        long actionId = withdrawPostingFrom(author, reporter);

        mvc.perform(post("/posts").cookie(author).param("body", "Blocked while withdrawn."))
                .andExpect(status().isForbidden());

        appeal(author, actionId, "They were my own projects, not adverts.");

        // One sign-in, reused. Each owner() spends a magic-link request, and §3.3 allows
        // three per address per hour — three calls in one test would sit exactly on the
        // limit and the next edit to this test would push it over.
        Cookie ownerSession = owner();
        // Scoped to this appeal's own words, not the first in the queue: the container
        // is shared, so the queue carries other suites' open appeals too.
        long appealId = appealIdFor(ownerSession, "They were my own projects, not adverts.");

        mvc.perform(post("/moderation/appeals/" + appealId).cookie(ownerSession)
                        .param("outcome", "UPHELD")
                        .param("reason", "Fair enough — they were his own."))
                .andExpect(status().is3xxRedirection());

        // The rung stops biting...
        mvc.perform(post("/posts").cookie(author).param("body", "Posting again after the appeal."))
                .andExpect(status().is3xxRedirection());
        // ...and the member is told, in the same statement-of-reasons shape.
        List<String> mail = mailBodiesFor("mod-app-up-a@example.org");
        assertThat(mail.getLast()).contains("Your appeal succeeded");
        // Nothing stands against them any more.
        assertThat(flat(standingSeenBy(author))).contains("Nothing has been taken from your account");
    }

    @Test
    void youCannotAppealADecisionThatWasNotAboutYou() throws Exception {
        Cookie author = completeMember("mod-app-else-a@example.org", "Other Author");
        Cookie reporter = completeMember("mod-app-else-b@example.org", "Other Reporter");
        Cookie bystander = completeMember("mod-app-else-c@example.org", "Passing Bystander");
        long actionId = withdrawPostingFrom(author, reporter);

        String refusal = mvc.perform(post("/appeals/" + actionId).cookie(bystander)
                        .param("account", "I would like to appeal this on his behalf."))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // Same words as a decision that does not exist: whether one does is not a
        // question a stranger gets answered (§8.5).
        assertThat(refusal).contains("no such decision");
    }
}
