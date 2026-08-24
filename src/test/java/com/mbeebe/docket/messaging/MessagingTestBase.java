package com.mbeebe.docket.messaging;

import com.mbeebe.docket.jobs.JobsTestBase;
import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Messaging-suite plumbing on top of the jobs and graph machinery (§7 needs
 * both: a Connection or an open Application authorises a Thread). The Postgres
 * container and GreenMail are shared across every suite in the run, so every
 * email here is prefixed "msg-" and every assertion is member- or row-scoped,
 * never a global count.
 */
public abstract class MessagingTestBase extends JobsTestBase {

    /** Sends one text Message and expects to land back on the Thread. */
    protected void send(Cookie author, long otherId, String body) throws Exception {
        mvc.perform(multipart("/messages/" + otherId).param("body", body).cookie(author))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/" + otherId));
    }

    protected String threadPage(Cookie viewer, long otherId) throws Exception {
        return mvc.perform(get("/messages/" + otherId).cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected String inboxPage(Cookie viewer) throws Exception {
        return mvc.perform(get("/messages").cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** The refusal a closed Thread gives back — the sentence itself is the assertion. */
    protected String refusalOfWriting(Cookie author, long otherId) throws Exception {
        return mvc.perform(multipart("/messages/" + otherId).param("body", "Anything")
                        .cookie(author))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getErrorMessage();
    }

    protected void disconnect(Cookie member, long otherId) throws Exception {
        mvc.perform(post("/p/" + otherId + "/disconnect").cookie(member))
                .andExpect(status().is3xxRedirection());
    }

    protected void block(Cookie member, long otherId) throws Exception {
        mvc.perform(post("/p/" + otherId + "/block").cookie(member))
                .andExpect(status().is3xxRedirection());
    }
}
