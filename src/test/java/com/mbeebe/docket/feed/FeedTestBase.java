package com.mbeebe.docket.feed;

import com.mbeebe.docket.graph.GraphTestBase;
import jakarta.servlet.http.Cookie;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Feed-suite helpers on top of the graph's connect/complete machinery. */
public abstract class FeedTestBase extends GraphTestBase {

    private static final Pattern POST_URL = Pattern.compile("/posts/(\\d+)");

    /**
     * Writes a Post and returns its id. Advances the stepping clock first: real
     * clocks never hand two Posts the same instant, but the test clock would.
     */
    protected long compose(Cookie session, String body) throws Exception {
        clock.advance(Duration.ofMinutes(1));
        String redirect = mvc.perform(post("/posts").cookie(session).param("body", body))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        Matcher matcher = POST_URL.matcher(redirect);
        if (!matcher.find()) {
            throw new AssertionError("Compose did not land on a post page: " + redirect);
        }
        return Long.parseLong(matcher.group(1));
    }

    /** One feed view — the page as this member sees it right now. */
    protected String feedSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
