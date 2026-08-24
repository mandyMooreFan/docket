package com.mbeebe.docket.company;

import com.mbeebe.docket.DocketTestBase;
import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.1: the Verified domain set is the identity key. Entities sharing a
 * verified domain ARE the same Company and auto-merge — positions and verifications
 * repointed, edit histories folded, the merge recorded as an audited, reversible
 * fact (§10.5).
 */
class AutoMergeTests extends DocketTestBase {

    private static final Pattern VERIFY_LINK = Pattern.compile("/verify/([A-Za-z0-9_-]+)");

    @Autowired
    Companies companies;

    @Autowired
    JdbcTemplate jdbc;

    long memberId(Cookie session) throws Exception {
        String url = mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

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
                .andExpect(status().is3xxRedirection());
        var messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = VERIFY_LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("No verify link in mail body:\n" + body);
        }
        mvc.perform(post("/verify").param("token", matcher.group(1)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void twoNamesForOneDomainBecomeOneCompany() throws Exception {
        // Two members fork the same employer under two names (§6.1's accepted cost)…
        Cookie first = memberAt("co-am-one@example.org", "Ann Acme", "Acme AM");
        Cookie second = memberAt("co-am-two@example.org", "Bob Acme", "Acme Ltd AM");
        long acme = companies.named("Acme AM").id();
        long acmeLtd = companies.named("Acme Ltd AM").id();
        assertThat(acme).isNotEqualTo(acmeLtd);

        // …then both demonstrate the same mail domain.
        verifyAt(first, acme, "ann@acme-am.example");
        mvc.perform(post("/companies/" + acmeLtd + "/verify").cookie(second)
                        .param("address", "bob@acme-am.example"))
                .andExpect(redirectedUrl("/companies/" + acmeLtd + "?verification=sent"));
        var messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = VERIFY_LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("No verify link");
        }
        // Consuming the second link lands on the SURVIVOR: they were one Company all along.
        mvc.perform(post("/verify").param("token", matcher.group(1)))
                .andExpect(redirectedUrl("/companies/" + acme + "?verification=done"));

        // The absorbed entity's URL redirects; it no longer stands alone.
        mvc.perform(get("/companies/" + acmeLtd))
                .andExpect(redirectedUrl("/companies/" + acme));

        // Both Positions now point at the survivor: both profiles link there, and
        // the survivor's people list carries both members.
        mvc.perform(get("/p/" + memberId(second)).cookie(second))
                .andExpect(content().string(containsString("/companies/" + acme)));
        mvc.perform(get("/companies/" + acme).cookie(first))
                .andExpect(content().string(containsString("2 members work here.")))
                .andExpect(content().string(containsString("Ann Acme")))
                .andExpect(content().string(containsString("Bob Acme")));

        // The merge is recorded as a fact: cause, direction, and every row it moved.
        Long mergeId = jdbc.queryForObject("""
                select id from company_merge
                where absorbed_company_id = %d and surviving_company_id = %d
                  and cause = 'SHARED_DOMAIN' and actor_member_id is null
                  and reversed_at is null
                """.formatted(acmeLtd, acme), Long.class);
        assertThat(mergeId).isNotNull();
        assertThat(jdbc.queryForObject("""
                select count(*) from company_merge_item
                where merge_id = %d and kind = 'POSITION'
                """.formatted(mergeId), Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from company_merge_item
                where merge_id = %d and kind = 'WORK_VERIFICATION'
                """.formatted(mergeId), Long.class)).isEqualTo(1);

        // Both verifications live on the survivor; the merged member passes the
        // trust gate there — their verification and current Position came along.
        assertThat(jdbc.queryForObject("""
                select count(*) from work_verification
                where company_id = %d and domain = 'acme-am.example'
                """.formatted(acme), Long.class)).isEqualTo(2);
        mvc.perform(get("/companies/" + acme + "/edit").cookie(second))
                .andExpect(status().isOk());
    }

    @Test
    void namingTheAbsorbedCompanyReusesTheSurvivorAndAutocompleteDropsIt() throws Exception {
        Cookie first = memberAt("co-am-name1@example.org", "Cee Dee", "Northgate AM");
        Cookie second = memberAt("co-am-name2@example.org", "Ef Gee", "Northgate Group AM");
        long northgate = companies.named("Northgate AM").id();
        long group = companies.named("Northgate Group AM").id();
        verifyAt(first, northgate, "cd@northgate-am.example");
        verifyAt(second, group, "fg@northgate-am.example");

        // Adding a Position under the dead name reaches the survivor, not a fork.
        assertThat(companies.named("Northgate Group AM").id()).isEqualTo(northgate);

        // The autocomplete no longer advertises the absorbed name (§6.1 reuse-first).
        mvc.perform(get("/companies/options").param("q", "Northgate").cookie(first))
                .andExpect(content().string(containsString("Northgate AM")))
                .andExpect(content().string(not(containsString("Northgate Group AM"))));
    }
}
