package com.mbeebe.docket.graph;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §4.2 and §7.3's endings at the graph level: Disconnect is quiet and
 * reversible; Block is total, durable, both directions — each Profile hidden
 * from the other with the same no-placeholder 404 as a member who does not
 * exist, and no request possible either way. Thread effects land with #36.
 */
class DisconnectAndBlockTests extends GraphTestBase {

    @Test
    void disconnectIsQuietAndReversible() throws Exception {
        Cookie ana = completeMember("dab-ana@example.org", "Ana Brook");
        Cookie bo = completeMember("dab-bo@example.org", "Bo Fenn");
        connect(ana, bo);
        long boId = memberId(bo);

        mvc.perform(post("/p/" + boId + "/disconnect").cookie(ana))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/p/" + boId));

        // Quiet: no trace on either side, and the door is open to ask again.
        mvc.perform(get("/p/" + boId).cookie(ana))
                .andExpect(content().string(containsString("name=\"note\"")))
                .andExpect(content().string(not(containsString("Disconnect"))));
        mvc.perform(get("/network").cookie(bo))
                .andExpect(content().string(not(containsString("Ana Brook"))));

        // Reversible: the handshake simply happens again.
        connect(ana, bo);
        mvc.perform(get("/p/" + boId).cookie(ana))
                .andExpect(content().string(containsString("Disconnect")));
    }

    @Test
    void blockHidesEachProfileFromTheOtherAndSeversTheConnection() throws Exception {
        Cookie cy = completeMember("dab-cy@example.org", "Cy Marsh");
        Cookie dot = completeMember("dab-dot@example.org", "Dot Hale");
        connect(cy, dot);
        long cyId = memberId(cy);
        long dotId = memberId(dot);

        mvc.perform(post("/p/" + dotId + "/block").cookie(cy))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/network"));

        // Total, both directions: for each, the other's Profile does not exist.
        mvc.perform(get("/p/" + dotId).cookie(cy)).andExpect(status().isNotFound());
        mvc.perform(get("/p/" + cyId).cookie(dot)).andExpect(status().isNotFound());

        // The Connection is severed in the same stroke, on both lists.
        mvc.perform(get("/network").cookie(cy))
                .andExpect(content().string(not(containsString("Dot Hale"))));
        mvc.perform(get("/network").cookie(dot))
                .andExpect(content().string(not(containsString("Cy Marsh"))));
    }

    @Test
    void aRequestAcrossABlockLooksLikeARequestToNobody() throws Exception {
        Cookie eli = completeMember("dab-eli@example.org", "Eli Wren");
        Cookie fay = completeMember("dab-fay@example.org", "Fay Dunn");
        long eliId = memberId(eli);
        long fayId = memberId(fay);

        mvc.perform(post("/p/" + fayId + "/block").cookie(eli))
                .andExpect(status().is3xxRedirection());

        // The blocked member's request answers exactly like a nonexistent one...
        mvc.perform(post("/p/" + eliId + "/connect").cookie(fay))
                .andExpect(status().isNotFound());
        mvc.perform(post("/p/999999999/connect").cookie(fay))
                .andExpect(status().isNotFound());
        // ...and the blocker cannot reach across either — durable, both ways.
        mvc.perform(post("/p/" + fayId + "/connect").cookie(eli))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockingASenderClearsTheirPendingRequest() throws Exception {
        Cookie gil = completeMember("dab-gil@example.org", "Gil Frost");
        Cookie hen = completeMember("dab-hen@example.org", "Hen Sable");
        long gilId = memberId(gil);
        long henId = memberId(hen);

        mvc.perform(post("/p/" + henId + "/connect").cookie(gil))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/p/" + gilId + "/block").cookie(hen))
                .andExpect(status().is3xxRedirection());

        // Nothing left to answer: the request is gone from the queue and inert.
        mvc.perform(get("/network").cookie(hen))
                .andExpect(content().string(containsString("No pending requests.")))
                .andExpect(content().string(not(containsString("Gil Frost"))));
        mvc.perform(post("/network/accept/" + gilId).cookie(hen))
                .andExpect(status().isNotFound());
    }
}
