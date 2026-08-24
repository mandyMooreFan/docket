package com.mbeebe.docket.graph;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §4.2: mutual Connection requests with the optional note, silent decline
 * with repeats blocked, and §9.2's asymmetry — an adult cannot send a request to
 * an under-18, who may send one to anyone. Compliance-shaped: held by tests.
 */
class ConnectionRequestTests extends GraphTestBase {

    @Test
    void aRequestCarriesItsNoteAndAcceptanceMakesAConnection() throws Exception {
        Cookie ada = completeMember("conn-ada@example.org", "Ada Lattice");
        Cookie ben = completeMember("conn-ben@example.org", "Ben Trellis");
        long adaId = memberId(ada);
        long benId = memberId(ben);

        // The affordance: a member who may connect sees the note-and-connect form.
        mvc.perform(get("/p/" + benId).cookie(ada))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"note\"")));

        mvc.perform(post("/p/" + benId + "/connect").cookie(ada)
                        .param("note", "We met at the standards meeting"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/p/" + benId));

        // The sender's view flips to "sent"; the form is gone.
        mvc.perform(get("/p/" + benId).cookie(ada))
                .andExpect(content().string(containsString("Connection request sent")))
                .andExpect(content().string(not(containsString("name=\"note\""))));

        // The recipient's /network lists the request, note and all.
        mvc.perform(get("/network").cookie(ben))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ada Lattice")))
                .andExpect(content().string(containsString("We met at the standards meeting")));

        // The sender's profile offers the recipient accept and decline directly.
        mvc.perform(get("/p/" + adaId).cookie(ben))
                .andExpect(content().string(containsString("/network/accept/" + adaId)));

        mvc.perform(post("/network/accept/" + adaId).cookie(ben))
                .andExpect(status().is3xxRedirection());

        // Connected, both ways round: each lists the other, the request is spent.
        mvc.perform(get("/network").cookie(ben))
                .andExpect(content().string(containsString("No pending requests.")))
                .andExpect(content().string(containsString("Ada Lattice")));
        mvc.perform(get("/network").cookie(ada))
                .andExpect(content().string(containsString("Ben Trellis")));
        mvc.perform(get("/p/" + benId).cookie(ada))
                .andExpect(content().string(containsString("Disconnect")));
    }

    @Test
    void declineIsSilentAndRepeatRequestsAreBlocked() throws Exception {
        Cookie cai = completeMember("conn-cai@example.org", "Cai Osier");
        Cookie dee = completeMember("conn-dee@example.org", "Dee Wattle");
        long caiId = memberId(cai);
        long deeId = memberId(dee);

        mvc.perform(post("/p/" + deeId + "/connect").cookie(cai))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/p/" + deeId));
        String beforeDecline = mvc.perform(get("/p/" + deeId).cookie(cai))
                .andReturn().getResponse().getContentAsString();
        assertThat(beforeDecline).contains("Connection request sent");

        mvc.perform(post("/network/decline/" + caiId).cookie(dee))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/network").cookie(dee))
                .andExpect(content().string(containsString("No pending requests.")));

        // The sender's view is byte-identical to before the decline: nothing leaks.
        String afterDecline = mvc.perform(get("/p/" + deeId).cookie(cai))
                .andReturn().getResponse().getContentAsString();
        assertThat(afterDecline).isEqualTo(beforeDecline);

        // A repeat is swallowed with the same outward response a fresh send gets...
        mvc.perform(post("/p/" + deeId + "/connect").cookie(cai)
                        .param("note", "Trying again"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/p/" + deeId));
        // ...but nothing reaches the recipient, and the sender's view still holds.
        mvc.perform(get("/network").cookie(dee))
                .andExpect(content().string(containsString("No pending requests.")))
                .andExpect(content().string(not(containsString("Trying again"))));
        String afterRepeat = mvc.perform(get("/p/" + deeId).cookie(cai))
                .andReturn().getResponse().getContentAsString();
        assertThat(afterRepeat).isEqualTo(beforeDecline);
    }

    @Test
    void connectingIsACapabilityAnIncompleteProfileHasNotEarned() throws Exception {
        Cookie eve = completeMember("conn-eve@example.org", "Eve Pleach");
        Cookie fresh = signUpAndIn("conn-fresh@example.org");
        long eveId = memberId(eve);

        // No affordance rendered, and the server refuses a forced request (§3.2).
        mvc.perform(get("/p/" + eveId).cookie(fresh))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"note\""))));
        mvc.perform(post("/p/" + eveId + "/connect").cookie(fresh))
                .andExpect(status().isForbidden());
        mvc.perform(get("/network").cookie(eve))
                .andExpect(content().string(containsString("No pending requests.")));

        // Signed out, connecting is just a door to the login page.
        mvc.perform(post("/p/" + eveId + "/connect"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void anAdultCannotRequestAnUnder18WhoMayRequestAnyone() throws Exception {
        Cookie minor = completeMinor("conn-minor@example.org", "Min Sixteen");
        Cookie adult = completeMember("conn-gus@example.org", "Gus Arbour");
        long minorId = memberId(minor);
        long adultId = memberId(adult);

        // The adult sees the members-only profile but gets no connect affordance...
        mvc.perform(get("/p/" + minorId).cookie(adult))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"note\""))));
        // ...and a forced request is refused server-side, from the stored Age fact (§9.2).
        mvc.perform(post("/p/" + minorId + "/connect").cookie(adult))
                .andExpect(status().isForbidden());
        mvc.perform(get("/network").cookie(minor))
                .andExpect(content().string(containsString("No pending requests.")));

        // The under-18 keeps their agency: they may send one to anyone.
        mvc.perform(get("/p/" + adultId).cookie(minor))
                .andExpect(content().string(containsString("name=\"note\"")));
        mvc.perform(post("/p/" + adultId + "/connect").cookie(minor))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/network").cookie(adult))
                .andExpect(content().string(containsString("Min Sixteen")));
        mvc.perform(post("/network/accept/" + minorId).cookie(adult))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/p/" + adultId).cookie(minor))
                .andExpect(content().string(containsString("Disconnect")));
    }

    @Test
    void aRequestToYourselfQuietlyDoesNothing() throws Exception {
        Cookie solo = completeMember("conn-solo@example.org", "Solo Quince");
        long soloId = memberId(solo);

        mvc.perform(post("/p/" + soloId + "/connect").cookie(solo))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/network").cookie(solo))
                .andExpect(content().string(containsString("No pending requests.")));
    }
}
