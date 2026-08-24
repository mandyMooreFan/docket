package com.mbeebe.docket;

import com.mbeebe.docket.invites.InviteTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §13.4, audited surface by surface. <strong>Empty-state copy is spec
 * surface</strong>, and the spec quotes the v1 baseline verbatim: each zero state
 * says what it is and points somewhere useful, editable in tone but not in
 * honesty. These are the six the spec names, asserted against its own words on
 * the rendered page — whitespace-normalised, because a template wraps where the
 * spec does not.
 *
 * <p>This suite lives at the root rather than in one module because §13.4 is one
 * rule spread across six modules, and the thing that goes wrong with copy like
 * this is drift in a corner nobody re-read.
 */
class EmptyStateTests extends InviteTestBase {

    private static final String FEED =
            "Your feed shows what your connections write — you don't have any connections "
                    + "yet. It stays empty until you do; nothing gets put here for you. "
                    + "Find someone you know, or invite them.";

    private static final String BOARD =
            "No open postings right now. Postings appear here the moment a member posts "
                    + "one — there's no backlog you can't see.";

    private static final String NO_PEOPLE =
            "Nobody by that name yet. People search matches names only — that's deliberate.";

    private static final String INBOX =
            "Messages open when you're connected to someone, or when someone applies to "
                    + "your posting. No connections yet — your inbox is waiting on the graph, "
                    + "not on you.";

    private static String flat(String html) {
        return html.replaceAll("\\s+", " ");
    }

    private String page(String url, Cookie session) throws Exception {
        return flat(mvc.perform(get(url).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void theEmptyFeedSaysWhatItIsAndPointsAtSearchTheInviteAndTheJobsBoard()
            throws Exception {
        Cookie alone = inviter("empty-alone@example.org", "Al One");

        String feed = page("/", alone);
        assertThat(feed).contains(FEED);
        // §13.4's three arrows off this state, and the Invite (§13.3) is the one
        // this ticket added — a member with no graph has two doors, and inviting
        // is the one search deliberately does not solve (§8.3).
        assertThat(feed).contains("href=\"/search\"")
                .contains("href=\"/invite\"")
                .contains("href=\"/jobs\"");
    }

    @Test
    void theRailAndItsNavDestinationsSayWhichEmptyTheyAre() throws Exception {
        Cookie quiet = inviter("empty-quiet@example.org", "Quinn Iet");

        // §2.3's rail, on the feed.
        String feed = page("/", quiet);
        assertThat(feed).contains("No jobs from your network yet.")
                .contains("No pending requests.");

        // §14.1: at phone width the rail does not stack, its contents are promoted
        // to nav destinations — so the pending-requests copy has to be there too.
        // §13.2's seeding mechanism stays reachable from here whatever the graph
        // looks like, which the feed's zero state cannot promise once it is gone.
        assertThat(page("/network", quiet)).contains("No pending requests.")
                .contains("href=\"/invite\"");
    }

    @Test
    void theEmptyJobsBoardSaysThereIsNoBacklogAndOffersTheSavedSearch() throws Exception {
        Cookie seeker = inviter("empty-seeker@example.org", "See Ker");

        // Logged out and signed in alike — the board is browsable either way (§8.4).
        String signedOut = flat(mvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(signedOut).contains(BOARD);

        String board = page("/jobs", seeker);
        assertThat(board).contains(BOARD);
        // §13.4's arrow off this one: the saved-search opt-in (§6.5).
        assertThat(board).contains("Save a search above");
    }

    @Test
    void searchWithNoResultsSaysWhyItMatchedNothing() throws Exception {
        Cookie looker = inviter("empty-looker@example.org", "Look Er");

        assertThat(page("/search?q=Zzyzxqqq", looker)).contains(NO_PEOPLE);
    }

    @Test
    void aCompanyPageWithOnePersonSaysSoAndPadsNothing() throws Exception {
        Cookie sole = employeeAt("empty-sole@example.org", "Sole Trader", "Empty State Forge");
        long companyId = companies.named("Empty State Forge").id();

        String company = page("/companies/" + companyId, sole);
        // §13.4: the page renders normally and the people list says "1 member
        // works here." No padding, and no "suggested companies" anywhere.
        assertThat(company).contains("1 member works here.")
                .contains(BOARD)
                .doesNotContain("Suggested").doesNotContain("Similar companies")
                .doesNotContain("You might also");
    }

    @Test
    void theInboxWithNothingWritableSaysItIsWaitingOnTheGraph() throws Exception {
        Cookie unwritable = inviter("empty-unwritable@example.org", "Un Writable");

        assertThat(page("/messages", unwritable)).contains(INBOX);
    }

    /**
     * The one honesty repair this ticket made to copy an earlier ticket had
     * already written. §13.4's inbox copy says "No connections yet" in as many
     * words, and it was rendering for every empty inbox — including a member who
     * has connections and simply has not been written to. Editable in tone, not
     * in honesty: that member gets a different, true sentence.
     */
    @Test
    void anEmptyInboxWithConnectionsIsNotToldItHasNone() throws Exception {
        Cookie one = inviter("empty-connected@example.org", "Con Nected");
        Cookie two = inviter("empty-other@example.org", "Oth Er");
        connect(one, two);

        String inbox = page("/messages", one);
        assertThat(inbox).doesNotContain(INBOX)
                .doesNotContain("No connections yet")
                .contains("Messages open when you're connected to someone, or when someone "
                        + "applies to your posting. Nothing has been written yet");
    }

    @Test
    void noZeroStateInventsSomethingToShow() throws Exception {
        Cookie fresh = inviter("empty-fresh@example.org", "Fre Sh");

        // §13.4 with §5.1 and §8.2 behind it: nothing is put in an empty part for
        // you. No suggestions, no people you may know, no promoted anything.
        for (String url : new String[] {"/", "/jobs", "/network", "/messages", "/invite"}) {
            assertThat(page(url, fresh))
                    .doesNotContain("People you may know")
                    .doesNotContain("Suggested")
                    .doesNotContain("Recommended for you")
                    .doesNotContain("Trending")
                    .doesNotContain("Promoted");
        }
    }
}
