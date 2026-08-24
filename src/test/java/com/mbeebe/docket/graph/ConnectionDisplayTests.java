package com.mbeebe.docket.graph;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §4.2's display rules: a Profile shows Mutuals and a connection count —
 * nothing else, no degrees — and the full connection list is visible to the
 * owner's Connections only. With a real graph, the Dial's connections-only
 * audience finally admits somebody.
 */
class ConnectionDisplayTests extends GraphTestBase {

    @Test
    void theCountShowsToEveryoneTheFullListToConnectionsOnly() throws Exception {
        Cookie owner = completeMember("disp-owner@example.org", "Orla Vane");
        Cookie one = completeMember("disp-one@example.org", "Pia Herring");
        Cookie two = completeMember("disp-two@example.org", "Quin Ossett");
        Cookie stranger = signUpAndIn("disp-stranger@example.org");
        connect(one, owner);
        connect(two, owner);
        long ownerId = memberId(owner);

        // Any member sees the count; the names stay with the owner's Connections.
        mvc.perform(get("/p/" + ownerId).cookie(stranger))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 connections")))
                .andExpect(content().string(not(containsString("Pia Herring"))))
                .andExpect(content().string(not(containsString("Quin Ossett"))));

        // The open web sees the count too — the profile is public and complete.
        mvc.perform(get("/p/" + ownerId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 connections")))
                .andExpect(content().string(not(containsString("Quin Ossett"))));

        // A Connection sees the full list; so does the owner.
        mvc.perform(get("/p/" + ownerId).cookie(one))
                .andExpect(content().string(containsString("Quin Ossett")));
        mvc.perform(get("/p/" + ownerId).cookie(owner))
                .andExpect(content().string(containsString("Pia Herring")))
                .andExpect(content().string(containsString("Quin Ossett")));
    }

    @Test
    void mutualsAreTheSharedConnectionsAndOnlyThose() throws Exception {
        Cookie hub = completeMember("disp-hub@example.org", "Hub Mercer");
        Cookie x = completeMember("disp-x@example.org", "Xan Poole");
        Cookie y = completeMember("disp-y@example.org", "Yara Slate");
        Cookie z = completeMember("disp-z@example.org", "Zed Crag");
        connect(hub, x);
        connect(hub, y);
        connect(z, x);
        long xId = memberId(x);

        // y and x share hub; z is x's alone, and no list is owed to a non-Connection.
        mvc.perform(get("/p/" + xId).cookie(y))
                .andExpect(content().string(containsString("<h2>Mutuals</h2>")))
                .andExpect(content().string(containsString("Hub Mercer")))
                .andExpect(content().string(not(containsString("Zed Crag"))));

        // Logged out there is nobody to share with: no Mutuals section at all.
        mvc.perform(get("/p/" + xId))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<h2>Mutuals</h2>"))));
    }

    @Test
    void theConnectionsOnlyDialNowAdmitsConnections() throws Exception {
        Cookie owner = completeMember("disp-dial@example.org", "Rex Dialler");
        Cookie friend = completeMember("disp-friend@example.org", "Sal Friend");
        Cookie other = signUpAndIn("disp-other@example.org");
        connect(friend, owner);
        long ownerId = memberId(owner);

        mvc.perform(post("/profile/dial").cookie(owner)
                        .param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        // Out of the audience the page does not exist — no placeholder (§4.1).
        mvc.perform(get("/p/" + ownerId)).andExpect(status().isNotFound());
        mvc.perform(get("/p/" + ownerId).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/p/" + ownerId).cookie(friend)).andExpect(status().isOk());
    }
}
