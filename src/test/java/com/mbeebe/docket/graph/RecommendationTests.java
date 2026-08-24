package com.mbeebe.docket.graph;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §4.3: only a Connection may write a Recommendation; it displays only
 * after the subject approves; the subject can hide it later. No reciprocal
 * prompts and no ask-for-one flow — requesting one is just a Message (#36).
 */
class RecommendationTests extends GraphTestBase {

    @Test
    void onlyAConnectionMayWriteOne() throws Exception {
        Cookie sue = completeMember("rec-sue@example.org", "Sue Ledger");
        Cookie con = completeMember("rec-con@example.org", "Con Abbey");
        Cookie stranger = completeMember("rec-stan@example.org", "Stan Frome");
        connect(con, sue);
        long sueId = memberId(sue);

        // No affordance for a non-Connection, and the server refuses the POST.
        mvc.perform(get("/p/" + sueId).cookie(stranger))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"text\""))));
        mvc.perform(post("/p/" + sueId + "/recommend").cookie(stranger)
                        .param("text", "Never actually worked with them"))
                .andExpect(status().isForbidden());

        // The Connection gets the serif form, and the words are accepted.
        mvc.perform(get("/p/" + sueId).cookie(con))
                .andExpect(content().string(containsString("name=\"text\"")));
        mvc.perform(post("/p/" + sueId + "/recommend").cookie(con)
                        .param("text", "A colleague worth following anywhere"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void itDisplaysOnlyAfterTheSubjectApprovesAndHidesOnTheirWord() throws Exception {
        Cookie sam = completeMember("rec-sam@example.org", "Sam Tarn");
        Cookie liv = completeMember("rec-liv@example.org", "Liv Marrow");
        Cookie other = signUpAndIn("rec-other@example.org");
        connect(liv, sam);
        long samId = memberId(sam);
        long livId = memberId(liv);
        String words = "Sam writes the clearest specs I have ever read";

        mvc.perform(post("/p/" + samId + "/recommend").cookie(liv)
                        .param("text", words))
                .andExpect(status().is3xxRedirection());

        // Unapproved, it is invisible everywhere — the author's view included.
        mvc.perform(get("/p/" + samId))
                .andExpect(content().string(not(containsString(words))));
        mvc.perform(get("/p/" + samId).cookie(other))
                .andExpect(content().string(not(containsString(words))));
        mvc.perform(get("/p/" + samId).cookie(liv))
                .andExpect(content().string(not(containsString(words))));

        // The subject sees it awaiting their approval, with the author's name.
        mvc.perform(get("/p/" + samId).cookie(sam))
                .andExpect(content().string(containsString(words)))
                .andExpect(content().string(containsString("Awaiting your approval")))
                .andExpect(content().string(containsString("Liv Marrow")));

        // Only the subject may approve: for anyone else there is nothing there.
        mvc.perform(post("/recommendations/" + livId + "/approve").cookie(other))
                .andExpect(status().isNotFound());

        mvc.perform(post("/recommendations/" + livId + "/approve").cookie(sam))
                .andExpect(status().is3xxRedirection());

        // Approved, it displays to whoever may see the Profile — the open web too.
        mvc.perform(get("/p/" + samId))
                .andExpect(content().string(containsString(words)))
                .andExpect(content().string(containsString("Liv Marrow")));
        mvc.perform(get("/p/" + samId).cookie(other))
                .andExpect(content().string(containsString(words)));

        // The subject can hide it later, and it is gone from every view.
        mvc.perform(post("/recommendations/" + livId + "/hide").cookie(sam))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/p/" + samId))
                .andExpect(content().string(not(containsString(words))));
        mvc.perform(get("/p/" + samId).cookie(sam))
                .andExpect(content().string(not(containsString(words))));
    }
}
