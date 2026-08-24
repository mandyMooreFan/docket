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

/** SPEC.md §4.1: education entries, and Skills as a self-declared plain list. */
class EducationAndSkillTests extends DocketTestBase {

    Cookie sessionWithBasics(String email, String name) throws Exception {
        Cookie session = signUpAndIn(email);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "School leaver")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    String ownProfileUrl(Cookie session) throws Exception {
        return mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
    }

    @Test
    void anEducationEntryAloneReachesTheCompletenessBar() throws Exception {
        Cookie session = sessionWithBasics("student@example.org", "Recent Graduate");
        String url = ownProfileUrl(session);
        mvc.perform(get(url)).andExpect(status().isNotFound());

        mvc.perform(post("/profile/education").cookie(session)
                        .param("institution", "Open University")
                        .param("course", "Mathematics")
                        .param("startYear", "2019").param("endYear", "2023"))
                .andExpect(status().is3xxRedirection());

        // §3.2: a Position or an education entry — the apprentice route counts fully.
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Open University")))
                .andExpect(content().string(containsString("Mathematics")));
    }

    @Test
    void anEducationEntryCanBeRemoved() throws Exception {
        Cookie session = sessionWithBasics("undo@example.org", "Undo Person");
        mvc.perform(post("/profile/education").cookie(session)
                        .param("institution", "Mistake Academy").param("course", "")
                        .param("startYear", "").param("endYear", ""))
                .andExpect(status().is3xxRedirection());
        String editPage = mvc.perform(get("/profile/edit").cookie(session))
                .andReturn().getResponse().getContentAsString();
        String id = editPage.replaceAll("(?s).*?/profile/education/(\\d+)/delete.*", "$1");

        mvc.perform(post("/profile/education/" + id + "/delete").cookie(session))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(not(containsString("Mistake Academy"))));
    }

    @Test
    void skillsAreAPlainSelfDeclaredList() throws Exception {
        Cookie session = sessionWithBasics("skilled@example.org", "Skilled Member");
        mvc.perform(post("/profile/skills").cookie(session).param("name", "Woodworking"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/skills").cookie(session).param("name", "Patience"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get(ownProfileUrl(session)).cookie(session))
                .andExpect(content().string(containsString("Woodworking")))
                .andExpect(content().string(containsString("Patience")));

        String editPage = mvc.perform(get("/profile/edit").cookie(session))
                .andReturn().getResponse().getContentAsString();
        String id = editPage.replaceAll("(?s).*?/profile/skills/(\\d+)/delete.*", "$1");
        mvc.perform(post("/profile/skills/" + id + "/delete").cookie(session))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void skillsDoNotCountTowardCompleteness() throws Exception {
        // §3.2 names a Position or education entry; a word in a list is neither.
        Cookie session = sessionWithBasics("wordy@example.org", "Wordy Member");
        mvc.perform(post("/profile/skills").cookie(session).param("name", "Completing profiles"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(ownProfileUrl(session))).andExpect(status().isNotFound());
    }
}
