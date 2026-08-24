package com.mbeebe.docket.invites;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §13.3, the landing half: what an Invite is worth at the far end.
 *
 * <p>Three rules are held here and they are the sharp ones. Signup is not gated
 * by an Invite and an invited signup differs from an ordinary one by exactly one
 * thing — a Connection request already waiting (§3.1). That request is an
 * ordinary §4.2 Connection request in every respect, decline included. And §9.2
 * governs it: an adult's waiting request for someone who turns out to be under 18
 * is the adult-to-minor request the spec forbids, so it silently never
 * materialises, with nothing anywhere for the adult to read the age fact off.
 */
class InviteLandingTests extends InviteTestBase {

    @Test
    void theInvitersRequestIsAlreadyWaitingWhenTheyJoin() throws Exception {
        Cookie ida = inviter("inv-ida@example.org", "Ida Osier");
        long idaId = memberId(ida);
        inviteAccepted(ida, "inv-joiner@example.org", "We met at the standards meeting.");

        Cookie joiner = joinAdultAt("inv-joiner@example.org");

        // There before they have done anything at all — note and all (§4.2).
        String network = networkPage(joiner);
        assertThat(network).contains("Ida Osier")
                .contains("We met at the standards meeting.");

        // And it is a real request, not a lookalike: accepting it connects them,
        // both ways round, in the graph everything else already reads.
        completeProfileOf(joiner, "Jo Newcomer");
        mvc.perform(post("/network/accept/" + idaId).cookie(joiner))
                .andExpect(status().is3xxRedirection());
        assertThat(networkPage(joiner)).contains("Ida Osier")
                .contains("No pending requests.");
        assertThat(networkPage(ida)).contains("Jo Newcomer");
        mvc.perform(get("/p/" + idaId).cookie(joiner))
                .andExpect(status().isOk());
    }

    @Test
    void signupIsNotGatedByAnInviteAndAnInvitedSignupDiffersOnlyByTheWaitingRequest()
            throws Exception {
        Cookie jed = inviter("inv-jed@example.org", "Jed Quince");
        inviteAccepted(jed, "inv-invited@example.org", "");

        // Both doors, side by side: the same neutral age ask, the same email step,
        // the same on-screen answer. An Invite buys the invitee nothing here, and
        // its absence costs the uninvited nothing (§3.1: signup stays open).
        String invitedAsk = mvc.perform(get("/join"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String uninvitedAsk = mvc.perform(get("/join"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(invitedAsk).isEqualTo(uninvitedAsk);

        String invitedStep = mvc.perform(post("/join/link")
                        .param("email", "inv-invited@example.org").param("ageKind", "ADULT"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String uninvitedStep = mvc.perform(post("/join/link")
                        .param("email", "inv-uninvited@example.org").param("ageKind", "ADULT"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(invitedStep).isEqualTo(uninvitedStep);

        Cookie invited = sessionCookieFor(magicLinkTokenFor("inv-invited@example.org"));
        Cookie uninvited = sessionCookieFor(magicLinkTokenFor("inv-uninvited@example.org"));

        // The uninvited member is a full member of a completely open product...
        assertThat(networkPage(uninvited)).contains("No pending requests.");
        mvc.perform(get("/").cookie(uninvited)).andExpect(status().isOk());
        mvc.perform(get("/jobs").cookie(uninvited)).andExpect(status().isOk());

        // ...and the one and only difference at the invited member's end is this.
        assertThat(networkPage(invited)).contains("Jed Quince");
    }

    @Test
    void theWaitingRequestDeclinesSilentlyAndBlocksARepeatLikeAnyOther() throws Exception {
        Cookie kit = inviter("inv-kit@example.org", "Kit Sallow");
        long kitId = memberId(kit);
        inviteAccepted(kit, "inv-decliner@example.org", "Do connect.");
        Cookie decliner = joinAdultAt("inv-decliner@example.org");
        long declinerId = memberId(decliner);

        String beforeDecline = mvc.perform(get("/p/" + declinerId).cookie(kit))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/network/decline/" + kitId).cookie(decliner))
                .andExpect(status().is3xxRedirection());

        // §4.2: silent. The inviter's view is byte-identical to before.
        assertThat(mvc.perform(get("/p/" + declinerId).cookie(kit))
                .andReturn().getResponse().getContentAsString()).isEqualTo(beforeDecline);
        assertThat(networkPage(decliner)).contains("No pending requests.");

        // §4.2: repeats blocked, by the Connect button and by a second Invite
        // alike — the Invite is the same request, so it hits the same wall.
        mvc.perform(post("/p/" + declinerId + "/connect").cookie(kit).param("note", "Again"))
                .andExpect(status().is3xxRedirection());
        inviteAccepted(kit, "inv-decliner@example.org", "Again");
        assertThat(networkPage(decliner)).contains("No pending requests.")
                .doesNotContain("Again");
    }

    /**
     * §9.2's sharpest corner. The honest options were: the request silently never
     * materialises, or it materialises in the one direction the spec allows. The
     * second is refused here. Flipping the direction puts a request in the young
     * person's name that they never sent, carrying an adult's words; and worse, it
     * tells the adult inviter — by a request arriving FROM their invitee rather
     * than waiting FOR them — that the person they invited is a child. That is
     * exactly the fact §9.2 exists to keep from exactly that adult. So: nothing
     * happens, nothing is recorded, and nothing the adult can see moved at all.
     */
    @Test
    void anAdultsWaitingRequestNeverMaterialisesForAnUnder18() throws Exception {
        Cookie lou = inviter("inv-lou@example.org", "Lou Arbour");
        long louId = memberId(lou);
        inviteAccepted(lou, "inv-minor@example.org", "You should be here.");

        // A control: the same adult invites an address nobody claims, so the two
        // sent rows are alike in everything except who turns up.
        inviteAccepted(lou, "inv-nobodyjoins@example.org", "You should be here.");
        String beforeAnyoneJoined = invitePage(lou);

        Cookie minor = completeProfileOf(joinMinorAt("inv-minor@example.org"), "Min Sixteen");
        long minorId = memberId(minor);

        // The 16-year-old joins into an ordinary, quiet product: no adult is
        // waiting for them, and the adult's note never reaches them either.
        assertThat(networkPage(minor)).contains("No pending requests.")
                .doesNotContain("Lou Arbour").doesNotContain("You should be here.");

        // And nothing on the adult's side moved when they joined: the Invite page
        // reads exactly as it did before, so the age fact is not inferable from it.
        assertThat(invitePage(lou)).isEqualTo(beforeAnyoneJoined);
        assertThat(networkPage(lou)).contains("No pending requests.");

        // The refusal is the graph's existing §9.2 rule, not a second one: the
        // adult pressing Connect on the same profile is refused the same way.
        mvc.perform(post("/p/" + minorId + "/connect").cookie(lou))
                .andExpect(status().isForbidden());

        // The young person keeps their agency — they may send one to anyone (§9.2).
        mvc.perform(post("/p/" + louId + "/connect").cookie(minor).param("note", "My choice"))
                .andExpect(status().is3xxRedirection());
        assertThat(networkPage(lou)).contains("Min Sixteen").contains("My choice");
    }

    @Test
    void anUnder18sOwnInviteDoesLeaveARequestWaitingForTheAdultWhoJoins() throws Exception {
        Cookie mai = completeMinor("inv-mai@example.org", "Mai Sixteen");
        inviteAccepted(mai, "inv-grownup@example.org", "From me, not to me.");

        Cookie grownup = joinAdultAt("inv-grownup@example.org");

        // §9.2 bars adults approaching under-18s, and nothing else. The asymmetry
        // is the point, so the Invite has to be asymmetric in the same direction.
        assertThat(networkPage(grownup)).contains("Mai Sixteen")
                .contains("From me, not to me.");
    }

    @Test
    void anInviteSurvivesAnAgeGateRefusalWithNothingStoredEitherSide() throws Exception {
        Cookie ned = inviter("inv-ned@example.org", "Ned Bracken");
        inviteAccepted(ned, "inv-tooyoung@example.org", "Come and see.");

        // §3.1: under 16 is refused at the first screen, before any email is
        // collected — so the Invite cannot even be told this happened.
        var tooYoung = java.time.YearMonth.now(clock).minusYears(14);
        mvc.perform(post("/join")
                        .param("month", String.valueOf(tooYoung.getMonthValue()))
                        .param("year", String.valueOf(tooYoung.getYear())))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("inv-tooyoung@example.org"));

        // Nothing is stored about the refusal, here as anywhere: the sender's page
        // is unchanged, and the Invite is simply still unspent.
        assertThat(invitePage(ned)).contains("inv-tooyoung@example.org");

        // Which is what "survives" means — whenever that address does become a
        // Member, the request is there, exactly as it always would have been.
        Cookie later = joinAdultAt("inv-tooyoung@example.org");
        assertThat(networkPage(later)).contains("Ned Bracken").contains("Come and see.");
    }

    @Test
    void everyInviterWhoAskedGetsTheirOwnWaitingRequest() throws Exception {
        Cookie ola = inviter("inv-ola@example.org", "Ola Withe");
        Cookie pip = inviter("inv-pip@example.org", "Pip Hazel");
        inviteAccepted(ola, "inv-popularjoiner@example.org", "From Ola.");
        inviteAccepted(pip, "inv-popularjoiner@example.org", "From Pip.");

        Cookie joiner = joinAdultAt("inv-popularjoiner@example.org");

        // Two separate §4.2 requests, exactly as if both had pressed Connect a
        // second after the join. Nothing merges, nothing is deduplicated.
        assertThat(networkPage(joiner)).contains("Ola Withe").contains("From Ola.")
                .contains("Pip Hazel").contains("From Pip.");
    }

    @Test
    void anInviteSpendsItselfOnceAndOnlyOnce() throws Exception {
        Cookie rex = inviter("inv-rex@example.org", "Rex Quill");
        long rexId = memberId(rex);
        inviteAccepted(rex, "inv-twice@example.org", "Once only.");
        Cookie joiner = joinAdultAt("inv-twice@example.org");

        mvc.perform(post("/network/decline/" + rexId).cookie(joiner))
                .andExpect(status().is3xxRedirection());

        // Signing in again is not joining again, so nothing re-lands: a spent
        // Invite cannot walk around §4.2's block on repeats after a decline.
        mvc.perform(post("/login/link").param("email", "inv-twice@example.org"))
                .andExpect(status().isOk());
        sessionCookieFor(magicLinkTokenFor("inv-twice@example.org"));
        assertThat(networkPage(joiner)).contains("No pending requests.");
    }
}
