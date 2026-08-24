package com.mbeebe.docket.messaging;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §7.1's other gate, on ADR-0001: an open Application authorises the
 * same one Thread a Connection would, scoped to the pair and reaching nobody
 * who did not apply (§6.3).
 *
 * <p>The window this suite pins down: open from the moment of applying while
 * the posting's window runs, open indefinitely once <strong>advanced</strong>
 * — because §7.4 spends its one accepted cost on the poster's reply arriving
 * only in the inbox, and a channel that shut on "advanced" would make that
 * cost unpayable — and closed by the Application's ending, whether that ending
 * is "not selected" or §6.4's close without response.
 */
class MessagingApplicationTests extends MessagingTestBase {

    @Autowired
    JdbcTemplate jdbc;

    long threadCount(long one, long other) {
        return jdbc.queryForObject(
                "select count(*) from thread where member_a = ? and member_b = ?",
                Long.class, Math.min(one, other), Math.max(one, other));
    }

    long applicationOn(long posting, String applicantEmail) {
        return jdbc.queryForObject("""
                select a.id from application a join member m on m.id = a.applicant_id
                where a.posting_id = %d and m.email = '%s'
                """.formatted(posting, applicantEmail), Long.class);
    }

    private void apply(Cookie applicant, long posting) throws Exception {
        mvc.perform(post("/jobs/" + posting + "/apply").cookie(applicant))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void anOpenApplicationOpensTheThreadForBothOfThem() throws Exception {
        Cookie poster = posterAt("msg-chan-poster@example.org", "Cass Poster",
                "Channelworks Ltd", "channelworks-msg.example");
        long posting = postJob(poster, companies.named("Channelworks Ltd").id(), "Bookbinder");
        Cookie applicant = completeMember("msg-chan-applicant@example.org", "Abe Applicant");
        long posterId = memberId(poster);
        long applicantId = memberId(applicant);
        apply(applicant, posting);

        // No Connection anywhere — the Application is the whole authorisation.
        send(poster, applicantId, "Your work on ledgers caught my eye.");
        clock.advance(Duration.ofMinutes(1));
        send(applicant, posterId, "Glad to hear it — happy to talk.");
        assertThat(threadCount(posterId, applicantId)).isEqualTo(1);
        assertThat(threadPage(applicant, posterId))
                .contains("Your work on ledgers caught my eye.");

        // §6.4's queue carries the affordance, and only for someone who applied.
        assertThat(mvc.perform(get("/jobs/" + posting + "/applications").cookie(poster))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .contains("/messages/" + applicantId);
    }

    @Test
    void aPosterCannotReachSomebodyWhoDidNotApply() throws Exception {
        Cookie poster = posterAt("msg-chan-cold@example.org", "Cole Poster",
                "Coldcallworks Ltd", "coldcallworks-msg.example");
        postJob(poster, companies.named("Coldcallworks Ltd").id(), "Sail Maker");
        Cookie bystander = completeMember("msg-chan-bystander@example.org", "Bea Bystander");
        long bystanderId = memberId(bystander);

        // §6.3, §7.1: nothing lets a poster contact members who did not apply.
        mvc.perform(multipart("/messages/" + bystanderId)
                        .param("body", "We're hiring, as it happens").cookie(poster))
                .andExpect(status().isForbidden());
        mvc.perform(get("/messages/" + bystanderId).cookie(poster))
                .andExpect(status().isNotFound());
        assertThat(threadCount(memberId(poster), bystanderId)).isZero();
    }

    @Test
    void advancingKeepsTheChannelOpenAndStillSendsNoMail() throws Exception {
        Cookie poster = posterAt("msg-chan-adv@example.org", "Ada Poster",
                "Advanceworks Ltd", "advanceworks-msg.example");
        long posting = postJob(poster, companies.named("Advanceworks Ltd").id(), "Typesetter");
        Cookie applicant = completeMember("msg-chan-advapp@example.org", "Ali Advanced");
        long applicantId = memberId(applicant);
        apply(applicant, posting);

        mvc.perform(post("/jobs/" + posting + "/applications/"
                        + applicationOn(posting, "msg-chan-advapp@example.org") + "/advance")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());

        // §7.4's accepted cost, made payable: advancing is the start of a
        // conversation, and the poster's actual words arrive only in the inbox.
        int mailBefore = mailBodiesFor("msg-chan-advapp@example.org").size();
        send(poster, applicantId, "We should find a time next week.");
        assertThat(mailBodiesFor("msg-chan-advapp@example.org")).hasSize(mailBefore);
        assertThat(threadPage(applicant, memberId(poster)))
                .contains("We should find a time next week.")
                .contains("Write a message");

        // And it survives the posting's own window closing: the Application,
        // not the posting, is what the channel follows.
        clock.advance(Duration.ofDays(31));
        sweepClosedPostings();
        send(applicant, memberId(poster), "Tuesday suits me.");
    }

    @Test
    void notSelectedEndsTheChannelAndTheHistoryStays() throws Exception {
        Cookie poster = posterAt("msg-chan-no@example.org", "Nils Poster",
                "Notselectedworks Ltd", "notselectedworks-msg.example");
        long posting = postJob(poster, companies.named("Notselectedworks Ltd").id(), "Cooper");
        Cookie applicant = completeMember("msg-chan-noapp@example.org", "Nan Notselected");
        long posterId = memberId(poster);
        long applicantId = memberId(applicant);
        apply(applicant, posting);
        send(poster, applicantId, "One question before I decide.");

        mvc.perform(post("/jobs/" + posting + "/applications/"
                        + applicationOn(posting, "msg-chan-noapp@example.org") + "/not-select")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());

        // §6.4: the Outcome is the ending, and the ending ends the channel —
        // in both directions, with the same one sentence a Disconnect gives.
        assertThat(refusalOfWriting(poster, applicantId))
                .isEqualTo("This thread is closed to new messages. "
                        + "The history stays here for both of you.");
        assertThat(refusalOfWriting(applicant, posterId))
                .isEqualTo(refusalOfWriting(poster, applicantId));
        assertThat(threadPage(applicant, posterId))
                .contains("One question before I decide.")
                .doesNotContain("Write a message");

        // The queue stops offering a door that is now shut.
        assertThat(mvc.perform(get("/jobs/" + posting + "/applications").cookie(poster))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .doesNotContain("/messages/" + applicantId);
    }

    @Test
    void closedWithoutResponseEndsTheChannelWithoutWaitingForTheSweep() throws Exception {
        Cookie poster = posterAt("msg-chan-silent@example.org", "Sil Poster",
                "Silenceworks Ltd", "silenceworks-msg.example");
        long posting = postJob(poster, companies.named("Silenceworks Ltd").id(), "Wheelwright");
        Cookie applicant = completeMember("msg-chan-silentapp@example.org", "Sam Silenced");
        long posterId = memberId(poster);
        long applicantId = memberId(applicant);
        apply(applicant, posting);
        send(applicant, posterId, "Applied today — here if it helps.");

        // Past the window's edge, before the hourly sweep has run: the answer is
        // derived from the window against the clock (ADR-0002), so it is already
        // the same answer the sweep will record.
        clock.advance(Duration.ofDays(31));
        mvc.perform(multipart("/messages/" + applicantId).param("body", "Too late now")
                        .cookie(poster))
                .andExpect(status().isForbidden());
        sweepClosedPostings();
        mvc.perform(multipart("/messages/" + posterId).param("body", "Any news?")
                        .cookie(applicant))
                .andExpect(status().isForbidden());

        // §7.3, §11.1: what was said is still there for both of them.
        assertThat(threadPage(applicant, posterId))
                .contains("Applied today — here if it helps.");
        assertThat(threadPage(poster, applicantId))
                .contains("Applied today — here if it helps.");
    }

    @Test
    void aPosterAndApplicantWhoLaterConnectSimplyContinueTheSameCorrespondence()
            throws Exception {
        Cookie poster = posterAt("msg-chan-both@example.org", "Bob Poster",
                "Bothgatesworks Ltd", "bothgatesworks-msg.example");
        long posting = postJob(poster, companies.named("Bothgatesworks Ltd").id(), "Founder");
        Cookie applicant = completeMember("msg-chan-bothapp@example.org", "Bay Applicant");
        long posterId = memberId(poster);
        long applicantId = memberId(applicant);
        apply(applicant, posting);
        send(poster, applicantId, "Written through the application.");

        // ADR-0001's own consequence: there is no second thread to reconcile.
        connect(applicant, poster);
        mvc.perform(post("/jobs/" + posting + "/applications/"
                        + applicationOn(posting, "msg-chan-bothapp@example.org")
                        + "/not-select").cookie(poster))
                .andExpect(status().is3xxRedirection());
        clock.advance(Duration.ofMinutes(1));
        send(poster, applicantId, "Written through the connection.");

        assertThat(threadCount(posterId, applicantId)).isEqualTo(1);
        assertThat(threadPage(applicant, posterId))
                .contains("Written through the application.")
                .contains("Written through the connection.");
    }
}
