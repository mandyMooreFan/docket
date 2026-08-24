package com.mbeebe.docket.identity;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** §3.3 magic links and §8.3's no-membership-oracle rule. */
class LoginAndLinkTests extends IdentityTestBase {

    @Test
    void theSameAnswerWhetherOrNotTheAddressHasAnAccount() throws Exception {
        signUpAndIn("known@example.org");
        greenMail.purgeEmailFromAllMailboxes();

        String forKnown = mvc.perform(post("/login/link").param("email", "known@example.org"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String forUnknown = mvc.perform(post("/login/link").param("email", "stranger@example.org"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(forUnknown).isEqualTo(forKnown);
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    void aLinkWorksExactlyOnce() throws Exception {
        String token = requestAdultJoinLink("once@example.org");
        sessionCookieFor(token);
        mvc.perform(post("/auth").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("doesn't work any more")));
    }

    @Test
    void aLinkExpiresAfterThirtyMinutes() throws Exception {
        String token = requestAdultJoinLink("late@example.org");
        clock.advance(Duration.ofMinutes(31));
        mvc.perform(post("/auth").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("doesn't work any more")));
    }

    @Test
    void theLinkLandsOnAConfirmationSoScannersCannotConsumeIt() throws Exception {
        String token = requestAdultJoinLink("careful@example.org");
        // A GET — what a scanner does — must not use the token up.
        mvc.perform(get("/auth/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("One more click")));
        sessionCookieFor(token);
    }
}
