package com.mbeebe.docket.graph;

import com.mbeebe.docket.DocketTestBase;
import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared plumbing for the graph suites: members complete enough to hold the
 * CONNECT capability (§3.2), and the request-then-accept handshake (§4.2).
 * The Postgres container is shared across every test class in the run, so
 * every email here is prefixed by its suite.
 */
public abstract class GraphTestBase extends DocketTestBase {

    protected Cookie completeMember(String email, String name) throws Exception {
        return complete(signUpAndIn(email), name);
    }

    protected Cookie completeMinor(String email, String name) throws Exception {
        return complete(signUpMinorAndIn(email), name);
    }

    private Cookie complete(Cookie session, String name) throws Exception {
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", "")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    protected long memberId(Cookie session) throws Exception {
        String url = mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

    /** The full §4.2 handshake: requester asks, recipient accepts. */
    protected void connect(Cookie requester, Cookie recipient) throws Exception {
        long recipientId = memberId(recipient);
        long requesterId = memberId(requester);
        mvc.perform(post("/p/" + recipientId + "/connect").cookie(requester))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/network/accept/" + requesterId).cookie(recipient))
                .andExpect(status().is3xxRedirection());
    }
}
