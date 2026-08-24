package com.mbeebe.docket.profile;

import com.mbeebe.docket.DocketTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §4.1 Positions and §6.1's creation rule: naming an employer while adding a
 * Position creates the Company, autocomplete-first so an existing one is reused, never
 * forked. Completeness (§3.2) turns on the first Position.
 */
class PositionTests extends DocketTestBase {

    Cookie sessionWithBasics(String email, String name) throws Exception {
        Cookie session = signUpAndIn(email);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name)
                        .param("headline", "Engineer")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    String ownProfileUrl(Cookie session) throws Exception {
        return mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
    }

    /** Positions are managed from the edit page; its forms carry the ids. */
    String firstPositionId(Cookie session) throws Exception {
        String editPage = mvc.perform(get("/profile/edit").cookie(session))
                .andReturn().getResponse().getContentAsString();
        return editPage.replaceAll("(?s).*?/profile/positions/(\\d+)/.*", "$1");
    }

    @Test
    void addingAPositionShowsItAndCompletesTheProfile() throws Exception {
        Cookie session = sessionWithBasics("worker@example.org", "Tom Deeds");
        String url = ownProfileUrl(session);

        // Name and headline alone don't reach the §3.2 bar: still invisible logged out.
        mvc.perform(get(url)).andExpect(status().isNotFound());

        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "Site reliability engineer")
                        .param("company", "Skyscanner")
                        .param("startMonth", "3").param("startYear", "2021")
                        .param("description", "Kept the searches searching."))
                .andExpect(status().is3xxRedirection());

        // Complete + adult + Dial at its public default: the page is on the open web.
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Site reliability engineer")))
                .andExpect(content().string(containsString("Skyscanner")))
                .andExpect(content().string(not(containsString("Finish your profile"))));
    }

    @Test
    void namingAnEmployerReusesTheExistingCompanyInsteadOfForkingIt() throws Exception {
        Cookie first = sessionWithBasics("one@example.org", "One");
        mvc.perform(post("/profile/positions").cookie(first)
                        .param("title", "Designer").param("company", "Marshwiggle Labs")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());

        Cookie second = sessionWithBasics("two@example.org", "Two");
        mvc.perform(post("/profile/positions").cookie(second)
                        .param("title", "Writer").param("company", "marshwiggle labs")
                        .param("startMonth", "2").param("startYear", "2022")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());

        // The autocomplete sees one Company, under its original casing — no fork.
        String options = mvc.perform(get("/companies/options").cookie(first).param("q", "marshw"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(options.split("Marshwiggle Labs", -1)).hasSize(2);
        assertThat(options).doesNotContain("marshwiggle labs");
    }

    @Test
    void aPositionNeedsNoCompanyAndCanBeEndedAndRemoved() throws Exception {
        Cookie session = sessionWithBasics("solo@example.org", "Solo Worker");
        // A blank title is refused the way a blank name is: it's a claim about a role.
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "  ").param("company", "")
                        .param("startMonth", "6").param("startYear", "2019")
                        .param("description", ""))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "Independent consultant").param("company", "")
                        .param("startMonth", "6").param("startYear", "2019")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(containsString("Independent consultant")));
        String positionId = firstPositionId(session);

        // Ending is self-reported (§16): the entry stays, dated as past.
        mvc.perform(post("/profile/positions/" + positionId + "/end").cookie(session)
                        .param("endMonth", "5").param("endYear", "2024"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(containsString("2024")));

        mvc.perform(post("/profile/positions/" + positionId + "/delete").cookie(session))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(not(containsString("Independent consultant"))));
    }

    @Test
    void youCannotTouchAnotherMembersPositions() throws Exception {
        Cookie owner = sessionWithBasics("mine@example.org", "Mine");
        mvc.perform(post("/profile/positions").cookie(owner)
                        .param("title", "Keeper").param("company", "")
                        .param("startMonth", "1").param("startYear", "2018")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        String positionId = firstPositionId(owner);

        Cookie intruder = signUpAndIn("intruder@example.org");
        mvc.perform(post("/profile/positions/" + positionId + "/delete").cookie(intruder))
                .andExpect(status().isNotFound());
        mvc.perform(get(ownProfileUrl(owner)).cookie(owner))
                .andExpect(content().string(containsString("Keeper")));
    }

    @Test
    void theCompanyAutocompleteIsForSignedInMembersOnly() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/companies/options").param("q", "any"))
                .andExpect(status().is3xxRedirection());
    }
}
