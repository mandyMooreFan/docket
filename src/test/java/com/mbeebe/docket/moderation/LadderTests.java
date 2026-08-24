package com.mbeebe.docket.moderation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3: the ladder withdraws what was abused. Proportionality is the point,
 * a capability never earned and one withdrawn are different states the Member is told
 * apart, and covert reach reduction is refused outright.
 */
class LadderTests extends ModerationTestBase {

    /** Report the author's post, then hand the queue back the report to act on. */
    private long reportedPostBy(Cookie author, Cookie reporter, String body) throws Exception {
        connect(author, reporter);
        long postId = compose(author, body);
        report(reporter, "POST", postId, "HARASSMENT", "This needs looking at.");
        return postId;
    }

    @Test
    void aWithdrawalTakesTheOneThingMisusedAndLeavesTheRest() throws Exception {
        Cookie author = completeMember("mod-lad-prop-a@example.org", "Proportional Author");
        Cookie reporter = completeMember("mod-lad-prop-b@example.org", "Proportional Reporter");
        long postId = reportedPostBy(author, reporter, "A post that led to a withdrawal.");
        Cookie ownerSession = owner();

        act(ownerSession, reportIdForPost(ownerSession, postId), "withdraw",
                "capability", "POST", "reason", "Repeatedly aimed at one person.");

        // The rung that was applied bites...
        mvc.perform(post("/posts").cookie(author).param("body", "Trying again."))
                .andExpect(status().isForbidden());
        // ...and §10.3's example holds: someone abusing posts keeps their correspondence.
        assertThat(threadPage(author, memberId(reporter))).doesNotContain("read-only");
        mvc.perform(get("/messages").cookie(author)).andExpect(status().isOk());
    }

    @Test
    void aWithdrawnCapabilityIsNotDescribedAsOneNeverEarned() throws Exception {
        Cookie author = completeMember("mod-lad-told-a@example.org", "Told Author");
        Cookie reporter = completeMember("mod-lad-told-b@example.org", "Told Reporter");
        long postId = reportedPostBy(author, reporter, "A post that gets its capability withdrawn.");
        Cookie ownerSession = owner();

        act(ownerSession, reportIdForPost(ownerSession, postId), "withdraw",
                "capability", "POST", "reason", "Aimed at one person, repeatedly.");

        // §10.3: "the member is told which they are in". The Profile here is complete,
        // so "finish your profile" would be both wrong and impossible to act on.
        String standing = flat(standingSeenBy(author));
        assertThat(standing).contains("A capability was withdrawn");
        assertThat(standing).contains("Aimed at one person, repeatedly.");
        assertThat(standing).doesNotContainIgnoringCase("not yet earned");
        assertThat(standing).doesNotContainIgnoringCase("complete your profile");
        assertThat(standing).doesNotContainIgnoringCase("profile is complete");
    }

    @Test
    void aMemberWithNothingAgainstThemIsToldThatPlainly() throws Exception {
        Cookie clean = completeMember("mod-lad-clean@example.org", "Clean Member");

        String standing = flat(standingSeenBy(clean));
        assertThat(standing).contains("Nothing has been taken from your account");
        // The never-earned side, said out loud rather than by absence.
        assertThat(standing).contains("has not been earned yet");
    }

    @Test
    void suspensionIsReadOnlyAndTheMemberCanStillSignInAndAppeal() throws Exception {
        Cookie author = completeMember("mod-lad-susp-a@example.org", "Suspended Author");
        Cookie reporter = completeMember("mod-lad-susp-b@example.org", "Suspending Reporter");
        long postId = reportedPostBy(author, reporter, "A post that leads to a suspension.");
        Cookie ownerSession = owner();

        act(ownerSession, reportIdForPost(ownerSession, postId), "suspend",
                "reason", "A pattern of it, not one post.");

        // Reading is untouched — the whole difference between rung 3 and rung 4.
        mvc.perform(get("/").cookie(author)).andExpect(status().isOk());
        mvc.perform(get("/messages").cookie(author)).andExpect(status().isOk());
        mvc.perform(get("/jobs").cookie(author)).andExpect(status().isOk());

        // Writing is not, including the writes §3.2 never gated as capabilities.
        mvc.perform(post("/posts").cookie(author).param("body", "Trying anyway."))
                .andExpect(status().isForbidden());
        mvc.perform(post("/profile/dial").cookie(author).param("dial", "PUBLIC"))
                .andExpect(status().isForbidden());

        // §10.3 gives one Appeal, and a suspension that swallowed it would make the
        // remedy imaginary — so this POST is deliberately still allowed through.
        appeal(author, appealableActionId(author), "The pattern was three years ago.");
    }

    @Test
    void removalIsTotalAndVisibleToTheAuthorToo() throws Exception {
        Cookie author = completeMember("mod-lad-rem-a@example.org", "Removed Author");
        Cookie reporter = completeMember("mod-lad-rem-b@example.org", "Removing Reporter");
        long postId = reportedPostBy(author, reporter, "A post that will be removed outright.");
        Cookie ownerSession = owner();

        act(ownerSession, reportIdForPost(ownerSession, postId), "remove",
                "reason", "Illegal content.");

        // §10.3 refuses shadowbanning: "covert reach reduction is a lie told to a member
        // about their own account". The author must find it gone too, not still there
        // for them and invisible to everyone else.
        mvc.perform(get("/posts/" + postId).cookie(author)).andExpect(status().isNotFound());
        mvc.perform(get("/posts/" + postId).cookie(reporter)).andExpect(status().isNotFound());
        assertThat(feedSeenBy(author)).doesNotContain("A post that will be removed outright.");
        assertThat(feedSeenBy(reporter)).doesNotContain("A post that will be removed outright.");
    }

    @Test
    void terminationEndsTheSessionsAsWellAsTheMember() throws Exception {
        Cookie author = completeMember("mod-lad-term-a@example.org", "Ending Author");
        Cookie reporter = completeMember("mod-lad-term-b@example.org", "Ending Reporter");
        long postId = reportedPostBy(author, reporter, "A post that ends an account.");
        Cookie ownerSession = owner();

        act(ownerSession, reportIdForPost(ownerSession, postId), "terminate",
                "reason", "Illegal content, not a first time.");

        // The door is closable from the other side: the Member is elsewhere when this
        // happens, so their cookie must simply stop working.
        mvc.perform(get("/messages").cookie(author))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void aDecidedReportCannotBeDecidedTwice() throws Exception {
        Cookie author = completeMember("mod-lad-twice-a@example.org", "Twice Author");
        Cookie reporter = completeMember("mod-lad-twice-b@example.org", "Twice Reporter");
        long postId = reportedPostBy(author, reporter, "A post decided once.");
        Cookie ownerSession = owner();
        long reportId = reportIdForPost(ownerSession, postId);

        act(ownerSession, reportId, "dismiss", "reason", "Nothing wrong with it.");

        mvc.perform(post("/moderation/reports/" + reportId + "/remove").cookie(ownerSession)
                        .param("reason", "Changed my mind."))
                .andExpect(status().isUnprocessableEntity());
    }
}
