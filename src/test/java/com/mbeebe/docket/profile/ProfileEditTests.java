package com.mbeebe.docket.profile;

import com.mbeebe.docket.DocketTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SPEC.md §4.1: editing the Profile's own words — name, headline, location, summary. */
class ProfileEditTests extends DocketTestBase {

    @Test
    void whatYouWriteOnTheEditPageShowsOnYourPage() throws Exception {
        Cookie session = signUpAndIn("editor@example.org");
        mvc.perform(get("/profile/edit").cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"name\"")));

        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", "Ada Lovelace")
                        .param("headline", "Analyst and metaphysician")
                        .param("location", "London")
                        .param("summary", "I write about engines."))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(containsString("Ada Lovelace")))
                .andExpect(content().string(containsString("Analyst and metaphysician")))
                .andExpect(content().string(containsString("London")))
                .andExpect(content().string(containsString("I write about engines.")));
    }

    @Test
    void theNameIsTheOnlyFieldWithAnyValidationAndItIsOnlyNonEmpty() throws Exception {
        Cookie session = signUpAndIn("nameless@example.org");
        // Everything blank but the name: accepted — pseudonyms and sparse pages are fine.
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", "Plumhoff")
                        .param("headline", "")
                        .param("location", "")
                        .param("summary", ""))
                .andExpect(status().is3xxRedirection());

        // A blank name is the one refusal, and nothing is overwritten by it.
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", "   ")
                        .param("headline", "x")
                        .param("location", "")
                        .param("summary", ""))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(containsString("Plumhoff")))
                .andExpect(content().string(not(containsString(">x<"))));
    }

    @Test
    void onlyTheSignedInMemberMayEditAndOnlyTheirOwnProfile() throws Exception {
        mvc.perform(get("/profile/edit")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/basics").param("name", "Drifter")
                        .param("headline", "").param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
    }

    String ownProfileUrl(Cookie session) throws Exception {
        return mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
    }
}
