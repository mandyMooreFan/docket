package com.mbeebe.docket.invites;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §13.3, the sending half: the mail itself, the §3.2 bar on the sender,
 * §3.3's inherited rate limits with no quota behind them, §8.3's refusal to
 * become a membership oracle, and §10.2's reportable target.
 */
class InviteTests extends InviteTestBase {

    @Autowired
    Invites invites;

    @Test
    void theInviteIsAnEmailToAStrangerCarryingTheSendersNameAndTheirNote() throws Exception {
        Cookie ada = inviter("inv-ada@example.org", "Ada Lattice");

        inviteAccepted(ada, "inv-stranger@example.org", "We worked together at the mill.");

        var bodies = mailBodiesFor("inv-stranger@example.org");
        assertThat(bodies).hasSize(1);
        assertThat(bodies.get(0))
                .contains("Ada Lattice")
                .contains("We worked together at the mill.")
                // §13.3 is "optional, never a gate", and the link proves it: the
                // open /join everyone else uses, with nothing to redeem. A token
                // here would be an invited signup route, which is the thing the
                // spec refuses (§3.1).
                .contains("/join")
                .doesNotContain("/auth/")
                .doesNotContain("invite=")
                .doesNotContain("token");
        // And it never hands the stranger the sender's own address.
        assertThat(bodies.get(0)).doesNotContain("inv-ada@example.org");
    }

    @Test
    void theSendersOwnPageListsWhereEachInviteWentAndNothingAboutWhatBecameOfIt()
            throws Exception {
        Cookie ben = inviter("inv-ben@example.org", "Ben Trellis");
        inviteAccepted(ben, "inv-recorded@example.org", "");

        String page = invitePage(ben);
        assertThat(page).contains("inv-recorded@example.org");
        // §8.3 told slowly is still §8.3: no "joined", "pending", "accepted".
        assertThat(page).doesNotContain("Joined").doesNotContain("Accepted")
                .doesNotContain("Declined").doesNotContain("Pending");
    }

    @Test
    void invitingIsWithheldUntilTheProfileIsComplete() throws Exception {
        Cookie fresh = signUpAndIn("inv-fresh@example.org");

        // §3.2's shape: no affordance rendered, and a forced send refused server-side.
        assertThat(invitePage(fresh))
                .contains("Inviting opens when your profile is complete.")
                .doesNotContain("name=\"email\"");
        invite(fresh, "inv-unreachable@example.org", "")
                .andExpect(status().isForbidden());
        assertThat(mailBodiesFor("inv-unreachable@example.org")).isEmpty();

        // Signed out it is simply a door to the login page.
        mvc.perform(post("/invite").param("email", "inv-unreachable@example.org"))
                .andExpect(redirectedUrl("/login"));
        assertThat(mailBodiesFor("inv-unreachable@example.org")).isEmpty();
    }

    @Test
    void anInviteToAnAddressThatIsAlreadyAMemberIsNoMembershipOracle() throws Exception {
        Cookie cai = inviter("inv-cai@example.org", "Cai Osier");
        Cookie dee = inviter("inv-dee@example.org", "Dee Wattle");

        // A control send to an address nobody has, then the same send to Dee's.
        String toStranger = invite(cai, "inv-nobody@example.org", "Join us")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invite?sent"))
                .andReturn().getResponse().getContentAsString();
        String toMember = invite(cai, "inv-dee@example.org", "Join us")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invite?sent"))
                .andReturn().getResponse().getContentAsString();

        // Byte-identical answers: nothing the sender can see says which was which.
        assertThat(toMember).isEqualTo(toStranger);

        // The stranger was written to; the member was not, and nothing reached her
        // by another door either — an Invite is not a back way to a request (§4.2).
        assertThat(mailBodiesFor("inv-nobody@example.org")).hasSize(1);
        assertThat(mailBodiesFor("inv-dee@example.org")).noneMatch(
                body -> body.contains("invited you to Docket"));
        assertThat(networkPage(dee)).contains("No pending requests.");

        // The sender's own page cannot be read as an oracle either: both rows,
        // identical but for the address.
        String page = invitePage(cai);
        assertThat(page).contains("inv-nobody@example.org").contains("inv-dee@example.org");

        // And the ledger moved for both, so the limits cannot be used to probe (§8.3).
        assertThat(invites.sentBy(memberId(cai))).hasSize(2);
        assertThat(invites.sentTo("inv-dee@example.org")).hasSize(1);
    }

    @Test
    void theRateLimitIsPerSenderAndPerAddressAndIsNotAQuota() throws Exception {
        Cookie eve = inviter("inv-eve@example.org", "Eve Pleach");

        // Per address first: a fourth Invite to one inbox within the hour stops.
        for (int i = 0; i < InviteService.MAX_PER_ADDRESS_PER_HOUR; i++) {
            inviteAccepted(eve, "inv-popular@example.org", "");
        }
        invite(eve, "inv-popular@example.org", "")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Too many invites")));
        assertThat(mailBodiesFor("inv-popular@example.org"))
                .hasSize(InviteService.MAX_PER_ADDRESS_PER_HOUR);

        // Then per sender, spread across fresh addresses so only the sender's own
        // budget can be what runs out.
        int sent = InviteService.MAX_PER_ADDRESS_PER_HOUR;
        int address = 0;
        while (sent < InviteService.MAX_PER_SENDER_PER_HOUR) {
            inviteAccepted(eve, "inv-crowd" + address++ + "@example.org", "");
            sent++;
        }
        invite(eve, "inv-onemore@example.org", "")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Too many invites")));
        assertThat(mailBodiesFor("inv-onemore@example.org")).isEmpty();

        // It is a rate limit, not §13.3's refused quota: the window moving on
        // restores the sender in full, with no lifetime total anywhere.
        clock.advance(java.time.Duration.ofHours(2));
        inviteAccepted(eve, "inv-onemore@example.org", "");
        assertThat(mailBodiesFor("inv-onemore@example.org")).hasSize(1);

        // And the limit is one sender's, not the product's: another member is
        // untouched by how enthusiastic Eve has been.
        Cookie fay = inviter("inv-fay@example.org", "Fay Withe");
        inviteAccepted(fay, "inv-elsewhere@example.org", "");
        assertThat(mailBodiesFor("inv-elsewhere@example.org")).hasSize(1);
    }

    @Test
    void aPublicInboxDomainIsRefusedTheSameWayTheSignupDoorRefusesIt() throws Exception {
        Cookie gus = inviter("inv-gus@example.org", "Gus Arbour");

        invite(gus, "anyone@mailinator.com", "")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("inboxes are public")));
        invite(gus, "not an address", "")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("look like an email address")));

        // Nothing refused reaches the ledger, so a refusal costs no budget either.
        assertThat(invites.sentBy(memberId(gus))).isEmpty();

        // §3.3's private aliases stay allowed, here as at the signup door.
        inviteAccepted(gus, "inv-alias@duck.com", "");
        assertThat(mailBodiesFor("inv-alias@duck.com")).hasSize(1);
    }

    @Test
    void theInviteIsAReportableTargetCarryingTheFactsAReportWouldName() throws Exception {
        Cookie hal = inviter("inv-hal@example.org", "Hal Withy");
        long halId = memberId(hal);
        inviteAccepted(hal, "inv-reporter@example.org", "Unwanted words.");

        // §10.2: the report about an Invite arrives from a non-member, by the
        // published contact address, naming an inbox and a date. That has to be
        // enough to find the row and the sender (#38 owns the queue itself).
        var found = invites.sentTo("INV-Reporter@example.org");
        assertThat(found).hasSize(1);
        var reported = found.get(0);
        assertThat(reported.senderId()).isEqualTo(halId);
        assertThat(reported.recipientEmail()).isEqualTo("inv-reporter@example.org");
        assertThat(reported.note()).isEqualTo("Unwanted words.");
        assertThat(reported.sentAt()).isBetween(clock.instant().minusSeconds(1),
                clock.instant().plusSeconds(1));

        // The id is stable, and it is the handle a queue entry would hold.
        assertThat(invites.reportable(reported.id())).contains(reported);
        assertThat(invites.reportable(reported.id() + 100_000)).isEmpty();
    }
}
