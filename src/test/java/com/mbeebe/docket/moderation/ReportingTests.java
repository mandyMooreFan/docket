package com.mbeebe.docket.moderation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.2: what is reportable and what is not. Profiles, Posts, Replies, Messages
 * in a Thread you are part of, Job postings and Companies — and nothing inside a Thread
 * you are not part of, because private is private by construction.
 */
class ReportingTests extends ModerationTestBase {

    @Test
    void reportingSomethingYouCanSeePutsItInTheQueueAndAcknowledgesAtOnce() throws Exception {
        Cookie author = completeMember("mod-rep-author@example.org", "Reported Author");
        Cookie reporter = completeMember("mod-rep-reporter@example.org", "Careful Reporter");
        connect(author, reporter);
        long postId = compose(author, "A post that someone objects to.");

        report(reporter, "POST", postId, "HARASSMENT", "This is aimed at me all week.");

        // DSA Art. 16's confirmation of receipt, in the same breath as the report.
        List<String> toReporter = mailBodiesFor("mod-rep-reporter@example.org");
        assertThat(toReporter).isNotEmpty();
        assertThat(toReporter.getLast()).contains("We received your report");
        // §10.1's honest expectation, in the acknowledgement rather than only on a page.
        assertThat(toReporter.getLast()).contains("one person reviews reports");

        String queue = queueSeenBy(owner());
        assertThat(queue).contains("This is aimed at me all week.");
        assertThat(queue).contains("Harassing someone");
    }

    @Test
    void aMessageInAThreadYouAreNotPartOfCannotBeReported() throws Exception {
        Cookie one = completeMember("mod-rep-priv-a@example.org", "Private One");
        Cookie two = completeMember("mod-rep-priv-b@example.org", "Private Two");
        Cookie outsider = completeMember("mod-rep-priv-c@example.org", "Curious Outsider");
        connect(one, two);
        send(one, memberId(two), "Something said in confidence.");

        // The outsider does not know the id, but knowing it must not help — walking the
        // space is exactly what §8.5 refuses, so every id answers the same way.
        for (long candidate = 1; candidate <= 5; candidate++) {
            mvc.perform(get("/report/MESSAGE/" + candidate).cookie(outsider))
                    .andExpect(status().isNotFound());
        }
        // 404, not 403: a refusal would confirm there is something there to refuse.
        mvc.perform(post("/report/MESSAGE/1").cookie(outsider)
                        .param("category", "HARASSMENT")
                        .param("account", "I would like to see this."))
                .andExpect(status().isNotFound());
    }

    @Test
    void aParticipantCanReportAMessageInTheirOwnThread() throws Exception {
        Cookie author = completeMember("mod-rep-own-a@example.org", "Sending Member");
        Cookie recipient = completeMember("mod-rep-own-b@example.org", "Receiving Member");
        connect(author, recipient);
        send(author, memberId(recipient), "Something unwelcome in a message.");

        String thread = threadPage(recipient, memberId(author));
        assertThat(thread).contains("/report/MESSAGE/");
    }

    @Test
    void aPostYouCannotSeeCannotBeReported() throws Exception {
        Cookie author = completeMember("mod-rep-dial@example.org", "Dialled Author");
        Cookie stranger = completeMember("mod-rep-str@example.org", "Passing Stranger");
        long postId = compose(author, "Kept among connections only.");
        mvc.perform(post("/profile/dial").cookie(author).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/report/POST/" + postId).cookie(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void theQueueDoesNotExistForAnyoneButTheOwner() throws Exception {
        Cookie member = completeMember("mod-rep-nosy@example.org", "Nosy Member");

        // 404 rather than 403 throughout: a queue that refused you by name would tell
        // you it is there and roughly how much is in it.
        mvc.perform(get("/moderation").cookie(member)).andExpect(status().isNotFound());
        mvc.perform(get("/moderation/complaints").cookie(member)).andExpect(status().isNotFound());
        mvc.perform(get("/moderation/intimate-images").cookie(member)).andExpect(status().isNotFound());
        mvc.perform(get("/moderation")).andExpect(status().isNotFound());
    }

    @Test
    void theReportFormOffersTheConductPolicysCategoriesAndNoOthers() throws Exception {
        Cookie author = completeMember("mod-rep-cats-a@example.org", "Category Author");
        Cookie reporter = completeMember("mod-rep-cats-b@example.org", "Category Reporter");
        connect(author, reporter);
        long postId = compose(author, "Something to categorise.");

        String form = mvc.perform(get("/report/POST/" + postId).cookie(reporter))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // §10.6's six, and the enum is the only source of them.
        for (ReportCategory category : ReportCategory.values()) {
            assertThat(form).contains(category.label());
        }
        // Nothing that would let a reporter invent an offence the policy does not name.
        assertThat(form).doesNotContainIgnoringCase("other");
    }
}
