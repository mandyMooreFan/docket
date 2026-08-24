package com.mbeebe.docket.identity;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** §3.3: rate-limited link requests; public inboxes blocked, private aliases allowed. */
class RateLimitAndBlocklistTests extends IdentityTestBase {

    @Test
    void aFourthLinkForTheSameAddressWithinAnHourIsRefused() throws Exception {
        for (int i = 0; i < MagicLinkService.MAX_PER_ADDRESS_PER_HOUR; i++) {
            mvc.perform(post("/login/link").param("email", "eager@example.org"))
                    .andExpect(content().string(containsString("Check your inbox")));
        }
        mvc.perform(post("/login/link").param("email", "eager@example.org"))
                .andExpect(content().string(containsString("Too many link requests")));
    }

    @Test
    void oneAddressCannotBeUsedToHammerTheMailerFromOneMachine() throws Exception {
        int sent = 0;
        int address = 0;
        while (sent < MagicLinkService.MAX_PER_IP_PER_HOUR) {
            String email = "crowd" + address++ + "@example.org";
            for (int i = 0; i < MagicLinkService.MAX_PER_ADDRESS_PER_HOUR
                    && sent < MagicLinkService.MAX_PER_IP_PER_HOUR; i++, sent++) {
                mvc.perform(post("/join/link").param("email", email).param("ageKind", "ADULT"))
                        .andExpect(content().string(containsString("Check your inbox")));
            }
        }
        mvc.perform(post("/join/link").param("email", "onemore@example.org").param("ageKind", "ADULT"))
                .andExpect(content().string(containsString("Too many link requests")));
    }

    @Test
    void publicInboxDomainsAreBlockedWithAnHonestExplanation() throws Exception {
        mvc.perform(post("/join/link").param("email", "anyone@mailinator.com").param("ageKind", "ADULT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("inboxes are public")));
    }

    @Test
    void privateAliasServicesAreAllowed() throws Exception {
        mvc.perform(post("/join/link").param("email", "alias@duck.com").param("ageKind", "ADULT"))
                .andExpect(content().string(containsString("Check your inbox")));
    }

    @Test
    void nonsenseIsNotAnEmailAddress() throws Exception {
        mvc.perform(post("/join/link").param("email", "not an address").param("ageKind", "ADULT"))
                .andExpect(content().string(containsString("look like an email address")));
    }
}
