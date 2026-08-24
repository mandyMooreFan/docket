package com.mbeebe.docket.identity;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** §3.3: 90-day sliding sessions, the visible list, sign-out-everywhere. */
class SessionTests extends IdentityTestBase {

    @Test
    void signingInSetsASessionAndTheHomePageKnowsYou() throws Exception {
        Cookie session = signUpAndIn("present@example.org");
        mvc.perform(get("/").cookie(session))
                .andExpect(content().string(containsString("present@example.org")));
    }

    @Test
    void loggedOutTheHomePageIsTheLandingPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("without the gimmicks")));
    }

    @Test
    void theSessionListShowsEverySessionAndMarksThisDevice() throws Exception {
        Cookie first = signUpAndIn("multi@example.org");
        greenMail.purgeEmailFromAllMailboxes();
        mvc.perform(post("/login/link").param("email", "multi@example.org"));
        Cookie second = sessionCookieFor(latestMailedToken());

        mvc.perform(get("/settings/sessions").cookie(second))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This device")))
                .andExpect(content().string(containsString("Another device")));
        // The other session still works too.
        mvc.perform(get("/").cookie(first))
                .andExpect(content().string(containsString("multi@example.org")));
    }

    @Test
    void signOutEndsOnlyTheCurrentSession() throws Exception {
        Cookie first = signUpAndIn("leaver@example.org");
        greenMail.purgeEmailFromAllMailboxes();
        mvc.perform(post("/login/link").param("email", "leaver@example.org"));
        Cookie second = sessionCookieFor(latestMailedToken());

        mvc.perform(post("/settings/sessions/sign-out").cookie(second))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/").cookie(second))
                .andExpect(content().string(containsString("without the gimmicks")));
        mvc.perform(get("/").cookie(first))
                .andExpect(content().string(containsString("leaver@example.org")));
    }

    @Test
    void signOutEverywhereEndsThemAll() throws Exception {
        Cookie first = signUpAndIn("everywhere@example.org");
        greenMail.purgeEmailFromAllMailboxes();
        mvc.perform(post("/login/link").param("email", "everywhere@example.org"));
        Cookie second = sessionCookieFor(latestMailedToken());

        mvc.perform(post("/settings/sessions/sign-out-everywhere").cookie(second))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/").cookie(first))
                .andExpect(content().string(containsString("without the gimmicks")));
        mvc.perform(get("/").cookie(second))
                .andExpect(content().string(containsString("without the gimmicks")));
    }

    @Test
    void theNinetyDayWindowSlidesWithUse() throws Exception {
        Cookie session = signUpAndIn("sliding@example.org");
        clock.advance(Duration.ofDays(60));
        mvc.perform(get("/").cookie(session))
                .andExpect(content().string(containsString("sliding@example.org")));
        clock.advance(Duration.ofDays(60));
        // 120 days after signing in, but only 60 since last use: still in.
        mvc.perform(get("/").cookie(session))
                .andExpect(content().string(containsString("sliding@example.org")));
        clock.advance(Duration.ofDays(91));
        mvc.perform(get("/").cookie(session))
                .andExpect(content().string(containsString("without the gimmicks")));
    }

    @Test
    void theSessionsPageNeedsASession() throws Exception {
        mvc.perform(get("/settings/sessions"))
                .andExpect(status().is3xxRedirection());
    }
}
