package com.mbeebe.docket.profile;

import com.mbeebe.docket.DocketTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SPEC.md §4.1: the Profile page, and the §3.2 floor on an incomplete one. */
class ProfilePageTests extends DocketTestBase {

    /** /profile is the stable "me" address; it lands on the member's own public page. */
    String ownProfileUrl(Cookie session) throws Exception {
        return mvc.perform(get("/profile").cookie(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/p/*"))
                .andReturn().getResponse().getRedirectedUrl();
    }

    @Test
    void yourProfileExistsFromTheMomentYouJoinAndYouMayAlwaysSeeIt() throws Exception {
        Cookie session = signUpAndIn("fresh@example.org");
        String url = ownProfileUrl(session);
        mvc.perform(get(url).cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit profile")));
    }

    @Test
    void anIncompleteProfileIsMembersOnlyRegardlessOfTheDial() throws Exception {
        Cookie session = signUpAndIn("incomplete@example.org");
        String url = ownProfileUrl(session);

        // Another member may see it: the floor is members-only, not invisible.
        Cookie other = signUpAndIn("other@example.org");
        mvc.perform(get(url).cookie(other)).andExpect(status().isOk());

        // Logged out it does not exist — no placeholder, no sign-in wall.
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void theAppBarAvatarLeadsToYourProfile() throws Exception {
        Cookie session = signUpAndIn("avatar@example.org");
        mvc.perform(get("/").cookie(session))
                .andExpect(content().string(containsString("class=\"av av-32\" href=\"/profile\"")));
    }

    @Test
    void signedOutVisitorsAreSentToSignInFromTheMeAddress() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login*"));
    }
}
