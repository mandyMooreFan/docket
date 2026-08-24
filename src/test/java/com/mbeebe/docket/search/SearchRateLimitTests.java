package com.mbeebe.docket.search;

import com.mbeebe.docket.graph.GraphTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3: "Scraping is answered by rate limits on search — per account;
 * per IP logged-out — a system control applied to everyone, not moderation, so
 * it never enters the queue or the ladder."
 *
 * <p>Both halves are tested: that the limit works, and that it is the kind of
 * thing §10.3 says it is. Hitting it costs a member nothing but the wait — no
 * capability is withdrawn (§10.3's rung 2), nothing is hidden from anyone
 * (visibility limiting is refused outright), and the next window opens on the
 * clock rather than on a decision by a person.
 *
 * <p>Every email is prefixed "srchrl-"; the shared clock steps two hours between
 * tests, so every window here starts empty.
 */
class SearchRateLimitTests extends GraphTestBase {

    private int searchStatus(String query, Cookie... session) throws Exception {
        var request = get("/search").param("q", query);
        if (session.length > 0) {
            request = request.cookie(session[0]);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    @Test
    void anAccountGetsSixtySearchesEveryTenMinutes() throws Exception {
        Cookie member = completeMember("srchrl-member@example.org", "Ray Tellimit");

        for (int i = 1; i <= SearchRateLimit.PER_MEMBER; i++) {
            assertThat(searchStatus("Ombrifex" + i, member))
                    .as("search %d of the window", i).isEqualTo(200);
        }
        // Sixty-one is where the door closes — politely, and with the numbers.
        var refused = mvc.perform(get("/search").param("q", "Ombrifex").cookie(member))
                .andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getContentAsString();
        assertThat(refused).contains("60 searches every ten minutes");
        assertThat(refused).contains("Nothing has happened to your account.");

        // The window is a window: it opens again on the clock, with nobody
        // deciding anything and nothing to appeal.
        clock.advance(Duration.ofMinutes(11));
        assertThat(searchStatus("Ombrifex", member)).isEqualTo(200);
    }

    @Test
    void signedOutTheAddressIsWhatIsCounted() throws Exception {
        for (int i = 1; i <= SearchRateLimit.PER_IP_SIGNED_OUT; i++) {
            assertThat(searchStatus("Wrastleby" + i))
                    .as("signed-out search %d of the window", i).isEqualTo(200);
        }
        assertThat(searchStatus("Wrastleby")).isEqualTo(429);

        // A signed-in member is counted on their account, not on the address —
        // so one busy IP cannot spend somebody else's budget.
        Cookie member = completeMember("srchrl-shared-ip@example.org", "Sam Sharedip");
        assertThat(searchStatus("Wrastleby", member)).isEqualTo(200);
    }

    @Test
    void aQueryWithNothingToAskCostsNoBudget() throws Exception {
        // §8.5's floor answers with nothing and runs no query, so hammering it
        // is not a way to spend a limit — and, more to the point, a page that
        // renders no results can never consume the budget for one that does.
        for (int i = 0; i < SearchRateLimit.PER_IP_SIGNED_OUT + 10; i++) {
            assertThat(searchStatus("%")).isEqualTo(200);
        }
        assertThat(searchStatus("Wrastleby")).isEqualTo(200);
    }

    @Test
    void theLimitWithdrawsNothingAndHidesNothing() throws Exception {
        Cookie member = completeMember("srchrl-ladder@example.org", "Lad Derless");
        for (int i = 0; i <= SearchRateLimit.PER_MEMBER; i++) {
            mvc.perform(get("/search").param("q", "Quennelbract").cookie(member));
        }
        assertThat(searchStatus("Quennelbract", member)).isEqualTo(429);

        // §10.3: not the ladder. Nothing was removed, no Capability was
        // withdrawn, the account is not read-only, and the member's own
        // surfaces answer exactly as they did a minute ago.
        for (String page : new String[] {"/", "/jobs", "/network", "/profile/edit"}) {
            mvc.perform(get(page).cookie(member))
                    .andExpect(status().isOk());
        }
        // And nothing is hidden from anyone else either: another member can
        // still find them by name, because there is no reach dial to turn down.
        Cookie other = completeMember("srchrl-onlooker@example.org", "Ona Looker");
        assertThat(mvc.perform(get("/search").param("q", "Derless").cookie(other))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .contains("/p/" + memberId(member));
    }
}
