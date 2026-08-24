package com.mbeebe.docket.company;

import com.mbeebe.docket.DocketTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.1, §8.4–8.5, §13.4: the Company page. Logged-out it shows name, logo,
 * description and postings only; the people list is account-gated, derived from
 * published current Positions, honours the Dial, and never pads its count.
 */
class CompanyPageTests extends DocketTestBase {

    @Autowired
    Companies companies;

    long memberId(Cookie session) throws Exception {
        String url = mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

    Cookie completeMemberAt(String email, String name, String company) throws Exception {
        Cookie session = signUpAndIn(email);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        addPosition(session, company);
        return session;
    }

    void addPosition(Cookie session, String company) throws Exception {
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", company)
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void loggedOutSeesTheCompanyButNeverItsPeople() throws Exception {
        completeMemberAt("co-page-owner@example.org", "Paige Turner", "Skyline Books CP");
        long company = companies.named("Skyline Books CP").id();

        mvc.perform(get("/companies/" + company))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Skyline Books CP")))
                // §13.4-honest postings empty state; postings themselves are #35.
                .andExpect(content().string(containsString("No open postings")))
                // §8.4: the people list is account-gated — no name, no count.
                .andExpect(content().string(not(containsString("Paige Turner"))))
                .andExpect(content().string(not(containsString("member works here"))))
                .andExpect(content().string(not(containsString("members work here"))));
    }

    @Test
    void thePeopleListCountsOnlyCurrentPositionsAndNeverPads() throws Exception {
        completeMemberAt("co-people-1@example.org", "Ada Current", "Brightwell Labs CP");
        Cookie viewer = signUpAndIn("co-people-viewer@example.org");
        long company = companies.named("Brightwell Labs CP").id();

        // §13.4 exact copy: "1 member works here." — no padding, no suggestions.
        mvc.perform(get("/companies/" + company).cookie(viewer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 member works here.")))
                .andExpect(content().string(containsString("Ada Current")));

        // A second member whose Position there has ended does not appear (§16:
        // currency gates the people list, derived at read time).
        Cookie past = completeMemberAt("co-people-2@example.org", "Bea Gone", "Brightwell Labs CP");
        long pastId = memberId(past);
        String positionId = null;
        String editPage = mvc.perform(get("/profile/edit").cookie(past))
                .andReturn().getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern.compile("/profile/positions/(\\d+)/end").matcher(editPage);
        if (!matcher.find()) {
            throw new AssertionError("No end-position form on the edit page");
        }
        positionId = matcher.group(1);
        mvc.perform(post("/profile/positions/" + positionId + "/end").cookie(past)
                        .param("endMonth", "2").param("endYear", "2021"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/companies/" + company).cookie(viewer))
                .andExpect(content().string(containsString("1 member works here.")))
                .andExpect(content().string(not(containsString("Bea Gone"))));

        completeMemberAt("co-people-3@example.org", "Cal Present", "Brightwell Labs CP");
        mvc.perform(get("/companies/" + company).cookie(viewer))
                .andExpect(content().string(containsString("2 members work here.")))
                .andExpect(content().string(containsString("Cal Present")));
    }

    @Test
    void thePeopleListHonoursTheDial() throws Exception {
        Cookie hidden = completeMemberAt("co-dial-hidden@example.org", "Hedda Private",
                "Quietworks CP");
        mvc.perform(post("/profile/dial").cookie(hidden).param("dial", "CONNECTIONS_ONLY"))
                .andExpect(status().is3xxRedirection());
        Cookie viewer = signUpAndIn("co-dial-viewer@example.org");
        long company = companies.named("Quietworks CP").id();

        // §8.5: a member whose effective visibility excludes the viewer is absent —
        // and the count counts only what this view shows.
        mvc.perform(get("/companies/" + company).cookie(viewer))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Hedda Private"))))
                .andExpect(content().string(not(containsString("1 member works here."))));

        // Their own page still shows them to themselves.
        mvc.perform(get("/companies/" + company).cookie(hidden))
                .andExpect(content().string(containsString("Hedda Private")))
                .andExpect(content().string(containsString("1 member works here.")));
    }

    @Test
    void aMinorAppearsToSignedInViewersOnlyAndNeverLoggedOut() throws Exception {
        Cookie minor = signUpMinorAndIn("co-minor@example.org");
        mvc.perform(post("/profile/basics").cookie(minor)
                        .param("name", "Young Apprentice").param("headline", "Apprentice")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        addPosition(minor, "Apprentice Yard CP");
        long company = companies.named("Apprentice Yard CP").id();

        // The under-18 floor makes their Profile members-only (§9.2); a signed-in
        // viewer may see them here, the open web never does (§8.4 gates it anyway).
        mvc.perform(get("/companies/" + company).cookie(signUpAndIn("co-minor-viewer@example.org")))
                .andExpect(content().string(containsString("Young Apprentice")));
        mvc.perform(get("/companies/" + company))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Young Apprentice"))));
    }

    @Test
    void anUnknownCompanyIsNotFound() throws Exception {
        mvc.perform(get("/companies/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void aProfilePositionLinksToItsCompanyPage() throws Exception {
        Cookie session = completeMemberAt("co-link@example.org", "Lin Kedd", "Anchorline CP");
        long company = companies.named("Anchorline CP").id();
        long member = memberId(session);

        mvc.perform(get("/p/" + member))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/companies/" + company)));
    }
}
