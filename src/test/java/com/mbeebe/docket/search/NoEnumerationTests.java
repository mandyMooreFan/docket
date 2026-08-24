package com.mbeebe.docket.search;

import com.mbeebe.docket.jobs.JobsTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §8.5, the deliberately adversarial half: <b>"No enumeration surface
 * exists beyond these — nothing returns a set of people defined by their
 * properties rather than their name."</b>
 *
 * <p>Every test here tries to get a LIST OF PEOPLE out of Docket by some route
 * other than searching a name while signed in, and has to fail. The claim being
 * defended is structural, so the assertions are too: a Profile row is the only
 * thing on the results page that renders a {@code /p/} link, which makes
 * "did any people come back" a question about the HTML rather than about a
 * count somebody has to keep honest.
 *
 * <p>Assertions are on those links, never on names: the results form echoes the
 * query back into its own box, as any search box should, so asserting that a
 * name is absent from a page whose query IS that name would only ever be
 * testing the echo.
 *
 * <p>The Postgres container is shared across every suite in the run, so every
 * email is prefixed "srchne-" and every name and term is nonsense unique to
 * this class.
 */
class NoEnumerationTests extends JobsTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private String search(String query, Cookie... session) throws Exception {
        var request = get("/search").param("q", query);
        if (session.length > 0) {
            request = request.cookie(session[0]);
        }
        return mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Every profile link on one results page — the whole people surface, listed. */
    private List<String> profileLinks(String body) {
        return java.util.regex.Pattern.compile("/p/\\d+").matcher(body)
                .results().map(java.util.regex.MatchResult::group).distinct().toList();
    }

    @Test
    void thereIsNoBrowseAllDoor() throws Exception {
        Cookie member = completeMember("srchne-browse@example.org", "Brow Sealot");
        // A member exists to be found, so an empty answer below is a refusal to
        // list rather than an empty database.
        completeMember("srchne-browse-target@example.org", "Yollimund Bracewait");

        // No query at all; an empty one; whitespace; a single character. None of
        // them is a search, and none of them is a first page of the membership.
        String noParam = mvc.perform(get("/search").cookie(member))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(profileLinks(noParam)).isEmpty();
        assertThat(noParam).contains("There is no list to browse");

        for (String nothing : new String[] {"", "   ", "y"}) {
            assertThat(profileLinks(search(nothing, member)))
                    .as("a query of \"%s\" lists nobody", nothing)
                    .isEmpty();
        }

        // Two characters IS a search — the floor is a floor, not a ban.
        assertThat(profileLinks(search("Yollimund", member))).isNotEmpty();
    }

    @Test
    void wildcardsAndTsqueryOperatorsAreNotOperators() throws Exception {
        Cookie seeker = completeMember("srchne-trick@example.org", "Trix Seeker");
        Cookie one = completeMember("srchne-trick-a@example.org", "Krellenbosch Onlyone");
        Cookie two = completeMember("srchne-trick-b@example.org", "Wibbleflax Twoo");

        // Characters that are wildcards in a LIKE, or operators in a tsquery, or
        // both. None reaches the query planner: only letters and digits survive
        // extraction, so each of these carries nothing to ask.
        for (String trick : new String[] {"%", "*", "_", "''", ":*", "!", "|", "&", "()", "%%%"}) {
            assertThat(profileLinks(search(trick, seeker)))
                    .as("\"%s\" is not a wildcard", trick)
                    .isEmpty();
        }

        // A tsquery OR would broaden two narrow searches into one wide one. It
        // does not survive: the terms are ANDed, and nobody is called both.
        String ored = search("Krellenbosch | Wibbleflax", seeker);
        assertThat(profileLinks(ored)).isEmpty();
        // …while each name on its own still works, so the refusal above is the
        // operator being dropped, not the search being broken.
        assertThat(search("Krellenbosch", seeker)).contains("/p/" + memberId(one));
        assertThat(search("Wibbleflax", seeker)).contains("/p/" + memberId(two));
    }

    @Test
    void loggedOutPeopleSearchReturnsNothingWhateverTheQuery() throws Exception {
        Cookie person = completeMember("srchne-out@example.org", "Vorlandish Kettlewright");
        String link = "/p/" + memberId(person);

        // §8.4: the exact full name, a prefix, and a wildcard try — all of them
        // answer with the reason, and none of them answers with a person.
        for (String query : new String[] {"Vorlandish Kettlewright", "Vorland", "Kettle", "%"}) {
            String body = search(query);
            assertThat(body).as("\"%s\" logged out", query).doesNotContain(link);
            assertThat(profileLinks(body)).isEmpty();
        }
        assertThat(search("Vorlandish")).contains("People search requires an account");
    }

    @Test
    void theFloorsHoldOnAnExactNameMatch() throws Exception {
        Cookie seeker = completeMember("srchne-floor-seeker@example.org", "Flo Seeker");

        // §9.2: an under-18's Profile is never returned by people search — the
        // Dial does not enter into it, and neither does who is asking.
        Cookie minor = completeMinor("srchne-minor@example.org", "Quimbleforth Youngly");
        assertThat(search("Quimbleforth Youngly", seeker))
                .doesNotContain("/p/" + memberId(minor));

        // §3.2: an incomplete Profile is un-indexed regardless of the Dial —
        // mass registration has no payoff because the pages are not there to find.
        Cookie sparse = signUpAndIn("srchne-sparse@example.org");
        mvc.perform(post("/profile/basics").cookie(sparse)
                        .param("name", "Grastlebum Halfdone").param("headline", "")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        assertThat(search("Grastlebum Halfdone", seeker))
                .doesNotContain("/p/" + memberId(sparse));

        // §8.5: the Dial itself, on a complete adult Profile. A stranger gets
        // nothing; the Connection the Dial admits gets the row — so the absence
        // above is the rule running, not the search failing.
        Cookie closed = completeMember("srchne-dial@example.org", "Threnody Shutterby");
        mvc.perform(post("/profile/dial").cookie(closed)
                        .param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());
        String closedLink = "/p/" + memberId(closed);
        assertThat(search("Threnody Shutterby", seeker)).doesNotContain(closedLink);

        Cookie friend = completeMember("srchne-dial-friend@example.org", "Ferd Dialfriend");
        connect(friend, closed);
        assertThat(search("Threnody Shutterby", friend)).contains(closedLink);
    }

    @Test
    void attributesAreNotAnAxisAndTheSkillWordIsNotEither() throws Exception {
        Cookie subject = signUpAndIn("srchne-attr@example.org");
        mvc.perform(post("/profile/basics").cookie(subject)
                        .param("name", "Perrindale Nobbs")
                        .param("headline", "Chief Zibblewright")
                        .param("location", "Grunwaldton")
                        .param("summary", "Twenty years of zibblewright work in Grunwaldton."))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(subject)
                        .param("title", "Zibblewright").param("company", "Nobbs Zibble Co")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/skills").cookie(subject).param("name", "Marrowfluting"))
                .andExpect(status().is3xxRedirection());

        Cookie seeker = completeMember("srchne-attr-seeker@example.org", "Att Seeker");
        String link = "/p/" + memberId(subject);
        // §8.1: no filtering by skill, location, employer, seniority or
        // availability. None of these words can reach a Profile, because the
        // index holds the name column and nothing else.
        for (String attribute : new String[] {
                "Zibblewright", "Grunwaldton", "Marrowfluting", "Nobbs Zibble Co"}) {
            assertThat(search(attribute, seeker))
                    .as("\"%s\" is an attribute, not a name", attribute)
                    .doesNotContain(link);
        }
        // The name still reaches them, which is the whole of what §8.1 allows.
        assertThat(search("Perrindale", seeker)).contains(link);
    }

    @Test
    void onlyThePeopleGroupEverHandsBackAPerson() throws Exception {
        Cookie author = completeMember("srchne-rows@example.org", "Cassiver Rowsley");
        mvc.perform(post("/posts").cookie(author)
                        .param("body", "Thrumbolt lantern maintenance, again."))
                .andExpect(status().is3xxRedirection());

        Cookie seeker = completeMember("srchne-rows-seeker@example.org", "Row Seeker");
        // A term nobody is named after: the Post comes back, and it names its
        // author — but a post row is about the post, so the row carries no link
        // into a Profile and the page's people surface stays empty.
        String body = search("Thrumbolt", seeker);
        assertThat(body).contains("Thrumbolt lantern maintenance, again.");
        assertThat(body).contains("Cassiver Rowsley");
        assertThat(profileLinks(body)).isEmpty();
    }

    @Test
    void anAbsorbedCompanyIsNotACompanyAnyMore() throws Exception {
        // §6.1's auto-merge: two names, one verified mail domain, one Company.
        Cookie first = posterAt("srchne-merge-a@example.org", "Ada Mergeone",
                "Plumbast NE", "plumbast-ne.example");
        long survivor = companies.named("Plumbast NE").id();
        Cookie second = employeeAt("srchne-merge-b@example.org", "Bo Mergetwo",
                "Plumbast Limited NE");
        long absorbed = companies.named("Plumbast Limited NE").id();
        assertThat(absorbed).isNotEqualTo(survivor);
        verifyWorkAt(second, absorbed, "bo@plumbast-ne.example");

        // §10.5: the absorbed row survives so the merge stays reversible, but it
        // is not an entity search may hand anyone — the same rule autocomplete
        // keeps, so the two can never disagree about which Companies exist.
        String body = search("Plumbast", first);
        assertThat(body).contains("/companies/" + survivor);
        assertThat(body).doesNotContain("/companies/" + absorbed);
    }

    @Test
    void openToWorkIsNowhereNearAnIndexAndNeitherIsAnyAttribute() {
        // §8.1: "never a search axis and never appears in the index" — enforced
        // against the live schema rather than remembered. Nothing generated and
        // nothing indexed anywhere in the database may mention the column.
        assertThat(jdbc.queryForList("""
                select indexname from pg_indexes
                where schemaname = 'public' and indexdef ilike '%open_to_work%'
                """, String.class)).isEmpty();
        assertThat(jdbc.queryForList("""
                select table_name || '.' || column_name from information_schema.columns
                where table_schema = 'public'
                  and generation_expression ilike '%open_to_work%'
                """, String.class)).isEmpty();

        // And the people index really is the name column alone: §8.1's refused
        // axis cannot creep into the document later without failing here.
        List<String> peopleDocuments = jdbc.queryForList("""
                select generation_expression from information_schema.columns
                where table_schema = 'public' and table_name = 'profile'
                  and generation_expression is not null
                """, String.class);
        assertThat(peopleDocuments).isNotEmpty();
        for (String expression : peopleDocuments) {
            assertThat(expression.toLowerCase())
                    .as("the people index is the name and nothing else")
                    .doesNotContain("headline").doesNotContain("location")
                    .doesNotContain("summary").doesNotContain("open_to_work");
        }
    }
}
