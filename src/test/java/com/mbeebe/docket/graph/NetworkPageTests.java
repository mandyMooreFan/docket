package com.mbeebe.docket.graph;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The app bar's Network destination: pending incoming requests and your connection
 * list. §13.4: the empty parts say what they are, honestly — no padding, no
 * suggestions.
 */
class NetworkPageTests extends GraphTestBase {

    @Test
    void networkIsForSignedInMembersOnly() throws Exception {
        mvc.perform(get("/network"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void theEmptyStatesSayWhatTheyAre() throws Exception {
        mvc.perform(get("/network").cookie(signUpAndIn("net-empty@example.org")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No pending requests.")))
                .andExpect(content().string(containsString("No connections yet.")));
    }
}
