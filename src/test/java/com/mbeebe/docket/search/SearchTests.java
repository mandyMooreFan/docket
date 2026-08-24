package com.mbeebe.docket.search;

import com.mbeebe.docket.jobs.JobsTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §8: one box returning people, companies, posts and jobs. People by
 * name only (§8.1); ordering impersonal — same query, same order, for everyone
 * (§8.2); Mutuals displayed but never reordering; the graph a tickable filter;
 * the open web rules (§8.4); the Dial honoured on every surface (§8.5) with
 * §9.2/§9.4's under-18 exclusions. The Postgres container is shared across
 * every suite in the run, so every email is prefixed "srch-" and every name
 * and search term is nonsense unique to this class.
 */
class SearchTests extends JobsTestBase {

    private static final Pattern PERSON_LINK = Pattern.compile("/p/(\\d+)");

    private String search(String query, Cookie... session) throws Exception {
        var request = get("/search").param("q", query);
        if (session.length > 0) {
            request = request.cookie(session[0]);
        }
        return mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private List<Long> peopleOrder(String body) {
        List<Long> ids = new ArrayList<>();
        Matcher matcher = PERSON_LINK.matcher(body);
        while (matcher.find()) {
            ids.add(Long.parseLong(matcher.group(1)));
        }
        return ids;
    }

    @Test
    void oneBoxReturnsPeopleCompaniesPostsAndJobs() throws Exception {
        Cookie poster = posterAt("srch-box-poster@example.org", "Quorvath Boxley",
                "Quorvath Industries", "quorvath-sb.example");
        long company = companies.named("Quorvath Industries").id();
        long jobId = postJob(poster, company, "Quorvath Wrangler");
        mvc.perform(post("/posts").cookie(poster)
                        .param("body", "Notes on quorvath handling."))
                .andExpect(status().is3xxRedirection());

        Cookie seeker = completeMember("srch-box-seeker@example.org", "Sal Boxseeker");
        String body = search("Quorvath", seeker);

        assertThat(body).contains("People").contains("Companies")
                .contains("Posts").contains("Jobs");
        assertThat(body).contains("Quorvath Boxley")
                .contains("/p/" + memberId(poster));
        assertThat(body).contains("Quorvath Industries").contains("/companies/" + company);
        assertThat(body).contains("Notes on quorvath handling.").contains("/posts/");
        assertThat(body).contains("Quorvath Wrangler").contains("/jobs/" + jobId);
    }

    @Test
    void peopleMatchNamesOnlyNeverAttributes() throws Exception {
        Cookie session = signUpAndIn("srch-attr-person@example.org");
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", "Plain Persson")
                        .param("headline", "Senior Vexillographer")
                        .param("location", "Vexilloville")
                        .param("summary", "A vexillographer of long standing."))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", "")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());

        Cookie seeker = completeMember("srch-attr-seeker@example.org", "Ann Attrseeker");
        // The attribute words reach nobody: people search matches names only (§8.1).
        String byAttribute = search("Vexillographer", seeker);
        assertThat(byAttribute).doesNotContain("Plain Persson");
        // §13.4's no-results copy, verbatim — it is static template text, so it
        // reaches the page exactly as the spec writes it, apostrophe and all.
        assertThat(byAttribute).contains(
                "Nobody by that name yet. People search matches names only — that's deliberate.");
        // The name still works.
        assertThat(search("Persson", seeker)).contains("Plain Persson");
    }

    @Test
    void prefixMatchesTheStartOfEachNameWord() throws Exception {
        Cookie person = completeMember("srch-prefix-person@example.org", "Zyggurat Quandle");
        Cookie seeker = completeMember("srch-prefix-seeker@example.org", "Pre Fixseeker");
        // A name search is what people type while they are still typing, so
        // every term is a prefix — asserted on the row's link, not on the name,
        // which the form echoes back into its own box.
        String row = "/p/" + memberId(person);
        assertThat(search("Zygg", seeker)).contains("Zyggurat Quandle").contains(row);
        assertThat(search("Quand", seeker)).contains("Zyggurat Quandle").contains(row);
        assertThat(search("Zyggurat Quandle", seeker)).contains(row);
    }

    @Test
    void sameQueryReturnsTheSameOrderForEveryone() throws Exception {
        completeMember("srch-ord-a@example.org", "Wrellick Aa");
        completeMember("srch-ord-b@example.org", "Wrellick Bb");
        Cookie third = completeMember("srch-ord-c@example.org", "Wrellick Cc");

        Cookie viewerOne = completeMember("srch-ord-v1@example.org", "Vera Ordone");
        Cookie viewerTwo = completeMember("srch-ord-v2@example.org", "Vern Ordtwo");
        // Viewer one is connected to one of the results; viewer two to nobody.
        connect(viewerOne, third);

        List<Long> orderForOne = peopleOrder(search("Wrellick", viewerOne));
        List<Long> orderForTwo = peopleOrder(search("Wrellick", viewerTwo));
        assertThat(orderForOne).hasSize(3);
        // §8.2: textual match quality, never a function of who is asking.
        assertThat(orderForOne).isEqualTo(orderForTwo);
    }

    @Test
    void mutualsAreDisplayedButNeverReorder() throws Exception {
        Cookie alpha = completeMember("srch-mut-alpha@example.org", "Vantorix Alpha");
        Cookie beta = completeMember("srch-mut-beta@example.org", "Vantorix Beta");
        Cookie middle = completeMember("srch-mut-middle@example.org", "Shared Middlebury");
        Cookie viewer = completeMember("srch-mut-viewer@example.org", "Val Mutviewer");
        // The viewer shares Middlebury with Beta, and nobody with Alpha.
        connect(viewer, middle);
        connect(beta, middle);

        String body = search("Vantorix", viewer);
        // The shared Connection is named on the row (§8.2: displayed)…
        assertThat(body).contains("Shared Middlebury");
        // …but the order stays the impersonal one: equal textual match, id tiebreak.
        assertThat(body.indexOf("Vantorix Alpha")).isLessThan(body.indexOf("Vantorix Beta"));
        assertThat(peopleOrder(body)).containsExactly(memberId(alpha), memberId(beta));
    }

    @Test
    void onlyPeopleIShareAConnectionWithIsATickableFilter() throws Exception {
        Cookie prime = completeMember("srch-tick-prime@example.org", "Umbrelloq Prime");
        completeMember("srch-tick-second@example.org", "Umbrelloq Second");
        Cookie viewer = completeMember("srch-tick-viewer@example.org", "Tick Viewer");
        connect(viewer, prime);

        String unticked = search("Umbrelloq", viewer);
        assertThat(unticked).contains("Umbrelloq Prime").contains("Umbrelloq Second");

        String ticked = mvc.perform(get("/search").param("q", "Umbrelloq")
                        .param("connected", "on").cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(ticked).contains("Umbrelloq Prime").doesNotContain("Umbrelloq Second");

        // §13.4: when the seeker's own tick is what emptied the group, the page
        // says so. "Nobody by that name" would be a lie told by a filter.
        String emptied = mvc.perform(get("/search").param("q", "Umbrelloq Second")
                        .param("connected", "on").cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(emptied).contains("among the people you share a connection with");
        assertThat(emptied).doesNotContain("Nobody by that name yet");
        assertThat(emptied).doesNotContain("/p/" + memberId(prime));
    }

    @Test
    void loggedOutJobsCompaniesAndPostsAreSearchableButPeopleAreNot() throws Exception {
        Cookie poster = posterAt("srch-open-poster@example.org", "Orla Fenwhistle",
                "Fenharbour Ltd", "fenharbour-so.example");
        long company = companies.named("Fenharbour Ltd").id();
        long jobId = postJob(poster, company, "Harbourmaster Fenrole");
        mvc.perform(post("/posts").cookie(poster)
                        .param("body", "Just harbour notes today."))
                .andExpect(status().is3xxRedirection());

        // The open web (§8.4): jobs, companies and Posts answer logged-out.
        assertThat(search("Fenharbour")).contains("Fenharbour Ltd")
                .contains("/companies/" + company);
        assertThat(search("Fenrole")).contains("Harbourmaster Fenrole")
                .contains("/jobs/" + jobId);
        assertThat(search("harbour notes")).contains("Just harbour notes today.");

        // People search requires an account — said plainly, never dressed up as
        // an empty result (§8.4, §13.4). Asserted on the absence of the row and
        // of any profile link at all, not on the word the form echoes back.
        String people = search("Fenwhistle");
        assertThat(people).contains("People search requires an account");
        assertThat(people).doesNotContain("Orla Fenwhistle");
        assertThat(people).doesNotContain("/p/");
        assertThat(people).doesNotContain("Nobody by that name yet");
    }

    @Test
    void minorAuthoredPostsAreNeverInLoggedOutResults() throws Exception {
        Cookie minor = completeMinor("srch-minor-author@example.org", "Yindle Marrow");
        mvc.perform(post("/posts").cookie(minor)
                        .param("body", "Grellow spadework notes."))
                .andExpect(status().is3xxRedirection());

        // §9.4: nothing authored by an under-18 is ever visible logged-out.
        assertThat(search("Grellow")).doesNotContain("Grellow spadework notes.");
        // Members may read it — the cap is on the open web, not the membership.
        Cookie member = completeMember("srch-minor-reader@example.org", "Reed Minorreader");
        assertThat(search("Grellow", member)).contains("Grellow spadework notes.");
    }

    @Test
    void theDialGovernsPeopleAndPostResultsAlike() throws Exception {
        Cookie author = completeMember("srch-dial-author@example.org", "Torvane Ellsworth");
        mvc.perform(post("/posts").cookie(author)
                        .param("body", "The torvane memoirs continue apace."))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/dial").cookie(author)
                        .param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());

        // A stranger sees neither the Profile nor the Post (§8.5: every surface).
        Cookie stranger = completeMember("srch-dial-stranger@example.org", "Stan Dialstranger");
        assertThat(search("Torvane", stranger)).doesNotContain("Torvane Ellsworth");
        assertThat(search("memoirs", stranger)).doesNotContain("memoirs continue");

        // A Connection sees both — the Dial admits them.
        Cookie friend = completeMember("srch-dial-friend@example.org", "Fred Dialfriend");
        connect(friend, author);
        assertThat(search("Torvane", friend)).contains("Torvane Ellsworth");
        assertThat(search("memoirs", friend)).contains("memoirs continue");

        // Logged out: a connections-only Profile is off the open web entirely.
        assertThat(search("Torvane")).doesNotContain("Torvane Ellsworth");
        assertThat(search("memoirs")).doesNotContain("memoirs continue");
    }

    @Test
    void blockedPairsNeverSeeEachOtherInResults() throws Exception {
        Cookie aleph = completeMember("srch-blk-aleph@example.org", "Grimswold Aleph");
        Cookie beth = completeMember("srch-blk-beth@example.org", "Grimswold Beth");
        mvc.perform(post("/p/" + memberId(beth) + "/block").cookie(aleph))
                .andExpect(status().is3xxRedirection());

        // §7.3: total, both directions — for each, the other does not exist.
        String forAleph = search("Grimswold", aleph);
        assertThat(forAleph).contains("Grimswold Aleph").doesNotContain("Grimswold Beth");
        String forBeth = search("Grimswold", beth);
        assertThat(forBeth).contains("Grimswold Beth").doesNotContain("Grimswold Aleph");
    }

    @Test
    void theJobsGroupIsOpenPostingsOnly() throws Exception {
        Cookie poster = posterAt("srch-openjob-poster@example.org", "Jo Openjobs",
                "Snerdlum Works", "snerdlum-sj.example");
        long company = companies.named("Snerdlum Works").id();
        long jobId = postJob(poster, company, "Snerdlum Archivist");

        assertThat(search("Snerdlum Archivist"))
                .contains("Snerdlum Archivist").contains("/jobs/" + jobId);

        // §6.3's window closes; the search stops answering with it (§8: open
        // only), derived from the window against the clock with no sweep in
        // between. Asserted on the row's link: the title itself is the query,
        // and the form honestly echoes the query back into its own box.
        clock.advance(Duration.ofDays(31));
        assertThat(search("Snerdlum Archivist")).doesNotContain("/jobs/" + jobId);
    }
}
