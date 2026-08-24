package com.mbeebe.docket.company;

import com.mbeebe.docket.DocketTestBase;
import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.1–6.2 and §16: page editing is gated by BOTH a Work verification at one
 * of the Company's verified domains AND a current Position there — the verification
 * never lapses, the Position's currency is derived at read time, and every accepted
 * edit lands in the history that answers vandalism (§10.5).
 */
class CompanyEditTests extends DocketTestBase {

    private static final Pattern VERIFY_LINK = Pattern.compile("/verify/([A-Za-z0-9_-]+)");

    @Autowired
    Companies companies;

    @Autowired
    JdbcTemplate jdbc;

    Cookie memberAt(String email, String name, String company) throws Exception {
        Cookie session = signUpAndIn(email);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", company)
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    void verifyAt(Cookie session, long companyId, String address) throws Exception {
        mvc.perform(post("/companies/" + companyId + "/verify").cookie(session)
                        .param("address", address))
                .andExpect(redirectedUrl("/companies/" + companyId + "?verification=sent"));
        var messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = VERIFY_LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("No verify link in mail body:\n" + body);
        }
        mvc.perform(post("/verify").param("token", matcher.group(1)))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * GreenMail aggregates messages per mailbox, not chronologically across them —
     * so the login link after a time jump is found by recipient, not by position.
     */
    String latestAuthTokenTo(String recipient) throws Exception {
        var messages = greenMail.getReceivedMessages();
        for (int i = messages.length - 1; i >= 0; i--) {
            if (messages[i].getAllRecipients()[0].toString().equals(recipient)) {
                Matcher m = Pattern.compile("/auth/([A-Za-z0-9_-]+)")
                        .matcher(GreenMailUtil.getBody(messages[i]));
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        throw new AssertionError("No auth mail to " + recipient);
    }

    void endCurrentPosition(Cookie session) throws Exception {
        String editPage = mvc.perform(get("/profile/edit").cookie(session))
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("/profile/positions/(\\d+)/end").matcher(editPage);
        if (!m.find()) {
            throw new AssertionError("No end-position form");
        }
        mvc.perform(post("/profile/positions/" + m.group(1) + "/end").cookie(session)
                        .param("endMonth", "2").param("endYear", "2030"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void editingNeedsBothTheVerificationAndACurrentPosition() throws Exception {
        Cookie holder = memberAt("co-edit-gate@example.org", "Eddie Tor", "Lantern Works CE");
        long company = companies.named("Lantern Works CE").id();

        // Signed out: to the sign-in page.
        mvc.perform(get("/companies/" + company + "/edit"))
                .andExpect(redirectedUrl("/login"));

        // A current Position alone is not enough — the Company has no verified
        // domain yet, so nobody at all can edit it (§6.1).
        mvc.perform(get("/companies/" + company + "/edit").cookie(holder))
                .andExpect(status().isForbidden());
        mvc.perform(post("/companies/" + company + "/edit").cookie(holder)
                        .param("name", "Lantern Works CE").param("description", "x"))
                .andExpect(status().isForbidden());

        verifyAt(holder, company, "eddie@lantern-ce.example");

        // Both halves held: the gate opens.
        mvc.perform(get("/companies/" + company + "/edit").cookie(holder))
                .andExpect(status().isOk());

        // A verified member elsewhere holds nothing here (§6.2: capability, not a badge).
        Cookie elsewhere = memberAt("co-edit-other@example.org", "Alia Bee", "Otherplace CE");
        long other = companies.named("Otherplace CE").id();
        verifyAt(elsewhere, other, "alia@otherplace-ce.example");
        mvc.perform(get("/companies/" + company + "/edit").cookie(elsewhere))
                .andExpect(status().isForbidden());

        // Ending the Position removes the derived right at once (§16, ADR-0002) —
        // the verification fact is untouched.
        endCurrentPosition(holder);
        mvc.perform(get("/companies/" + company + "/edit").cookie(holder))
                .andExpect(status().isForbidden());
    }

    @Test
    void theVerificationNeverLapsesWhileThePositionRunsOn() throws Exception {
        Cookie holder = memberAt("co-edit-years@example.org", "Perma Nent", "Evergreen CE");
        long company = companies.named("Evergreen CE").id();
        verifyAt(holder, company, "perma@evergreen-ce.example");

        // Years pass. The session dies (90-day slide) but the dated fact does not.
        clock.advance(Duration.ofDays(4 * 365));
        mvc.perform(post("/login/link").param("email", "co-edit-years@example.org"))
                .andExpect(status().isOk());
        Cookie fresh = sessionCookieFor(latestAuthTokenTo("co-edit-years@example.org"));

        mvc.perform(get("/companies/" + company + "/edit").cookie(fresh))
                .andExpect(status().isOk());
        mvc.perform(post("/companies/" + company + "/edit").cookie(fresh)
                        .param("name", "Evergreen CE")
                        .param("description", "Still here, years on."))
                .andExpect(redirectedUrl("/companies/" + company));
        mvc.perform(get("/companies/" + company))
                .andExpect(content().string(containsString("Still here, years on.")));
    }

    @Test
    void editsChangeThePageAndLandInTheHistory() throws Exception {
        Cookie holder = memberAt("co-edit-hist@example.org", "Historia Penn", "Quillstone CE");
        long company = companies.named("Quillstone CE").id();
        verifyAt(holder, company, "hp@quillstone-ce.example");

        mvc.perform(post("/companies/" + company + "/edit").cookie(holder)
                        .param("name", "Quillstone Press CE")
                        .param("description", "A small press."))
                .andExpect(redirectedUrl("/companies/" + company));

        mvc.perform(get("/companies/" + company))
                .andExpect(content().string(containsString("Quillstone Press CE")))
                .andExpect(content().string(containsString("A small press.")));

        // Who, when, from what, to what (§6.1) — one row per changed field.
        assertThat(jdbc.queryForList("""
                select field || '|' || old_value || '|' || new_value
                from company_edit e join member m on m.id = e.member_id
                where e.company_id = %d and m.email = 'co-edit-hist@example.org'
                order by e.id
                """.formatted(company), String.class))
                .containsExactly("NAME|Quillstone CE|Quillstone Press CE",
                        "DESCRIPTION||A small press.");

        // The history page: members can answer vandalism from it; it is account-gated
        // like every people-shaped surface (§8.4).
        mvc.perform(get("/companies/" + company + "/history"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/companies/" + company + "/history")
                        .cookie(signUpAndIn("co-edit-hist-viewer@example.org")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historia Penn")))
                .andExpect(content().string(containsString("Quillstone CE")))
                .andExpect(content().string(containsString("Quillstone Press CE")));
    }

    @Test
    void aNameIsNeverBlankAndNeverForksAnotherCompany() throws Exception {
        memberAt("co-edit-taken@example.org", "Tay Ken", "Firstname CE");
        Cookie holder = memberAt("co-edit-namer@example.org", "Nate Amer", "Secondname CE");
        long company = companies.named("Secondname CE").id();
        verifyAt(holder, company, "nate@secondname-ce.example");

        mvc.perform(post("/companies/" + company + "/edit").cookie(holder)
                        .param("name", "  ").param("description", ""))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/companies/" + company + "/edit").cookie(holder)
                        .param("name", "Firstname CE").param("description", ""))
                .andExpect(status().isUnprocessableEntity());
        // Unchanged.
        mvc.perform(get("/companies/" + company))
                .andExpect(content().string(containsString("Secondname CE")));
    }
}
