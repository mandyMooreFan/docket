package com.mbeebe.docket.profile;

import com.mbeebe.docket.DocketTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §4.1: the one Dial, and the service-imposed floors it can never override.
 * These are the compliance-shaped rules the map wants held by tests, not by care.
 */
class DialAndFloorTests extends DocketTestBase {

    Cookie completeMember(String email, String name) throws Exception {
        Cookie session = signUpAndIn(email);
        complete(session, name);
        return session;
    }

    void complete(Cookie session, String name) throws Exception {
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", "")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
    }

    String ownProfileUrl(Cookie session) throws Exception {
        return mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
    }

    void setDial(Cookie session, String dial) throws Exception {
        mvc.perform(post("/profile/dial").cookie(session).param("dial", dial))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void theDialTurnsThroughItsThreeAudiences() throws Exception {
        Cookie owner = completeMember("dialler@example.org", "Dialler");
        Cookie other = signUpAndIn("watcher@example.org");
        String url = ownProfileUrl(owner);

        // Public by default: on the open web.
        mvc.perform(get(url)).andExpect(status().isOk());

        setDial(owner, "MEMBERS_ONLY");
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).cookie(other)).andExpect(status().isOk());

        // Connections-only: no graph exists yet (#32), so only the owner qualifies.
        setDial(owner, "CONNECTIONS_ONLY");
        mvc.perform(get(url).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get(url).cookie(owner)).andExpect(status().isOk());

        setDial(owner, "PUBLIC");
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void anUnder18sProfileIsMembersOnlyRegardlessOfTheDial() throws Exception {
        Cookie minor = signUpMinorAndIn("minor-dial@example.org");
        complete(minor, "Young Person");
        String url = ownProfileUrl(minor);

        // Complete, and the Dial sits at its PUBLIC default — the floor still holds.
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).cookie(signUpAndIn("adult@example.org"))).andExpect(status().isOk());

        // Turning the Dial to PUBLIC on purpose changes nothing.
        setDial(minor, "PUBLIC");
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void openToWorkDefaultsOffAndNeverRendersOutsideItsAudience() throws Exception {
        Cookie owner = completeMember("quiet@example.org", "Quiet Seeker");
        Cookie other = signUpAndIn("colleague@example.org");
        String url = ownProfileUrl(owner);

        // Off by default (§4.1) — nobody sees it, the owner included.
        mvc.perform(get(url).cookie(owner))
                .andExpect(content().string(not(containsString("Open to work"))));

        mvc.perform(post("/profile/open-to-work").cookie(owner).param("audience", "MEMBERS"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(url).cookie(other))
                .andExpect(content().string(containsString("Open to work")));
        // Never on the logged-out rendering: no audience includes the open web (§8.1).
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Open to work"))));

        // Connections-audience: no graph yet, so another member sees nothing.
        mvc.perform(post("/profile/open-to-work").cookie(owner).param("audience", "CONNECTIONS"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(url).cookie(other))
                .andExpect(content().string(not(containsString("Open to work"))));
        mvc.perform(get(url).cookie(owner))
                .andExpect(content().string(containsString("Open to work")));
    }
}
