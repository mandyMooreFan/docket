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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §6.2: the Work verification — a magic link to an address at the Company's
 * domain, the same machinery as login. A dated fact; a SECOND address, not the login
 * address; rate-limited like every outbound-mail source (§14.2).
 */
class WorkVerificationTests extends DocketTestBase {

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

    String latestVerifyToken() {
        var messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = VERIFY_LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("No verify link in mail body:\n" + body);
        }
        return matcher.group(1);
    }

    void consume(String token, long companyId) throws Exception {
        mvc.perform(get("/verify/" + token)).andExpect(status().isOk());
        mvc.perform(post("/verify").param("token", token))
                .andExpect(redirectedUrl("/companies/" + companyId + "?verification=done"));
    }

    @Test
    void theLinkLandsAtTheWorkAddressAndConsumingItStoresTheDatedFact() throws Exception {
        Cookie session = memberAt("co-wv-flow@example.org", "Vera Fied", "Harbourlight WV");
        long company = companies.named("Harbourlight WV").id();

        mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                        .param("address", "vera@harbourlight-wv.example"))
                .andExpect(redirectedUrl("/companies/" + company + "?verification=sent"));

        // The mail went to the WORK address — the second address, not the login one.
        var messages = greenMail.getReceivedMessages();
        assertThat(messages[messages.length - 1].getAllRecipients()[0].toString())
                .isEqualTo("vera@harbourlight-wv.example");

        consume(latestVerifyToken(), company);

        // The durable fact: member, company, domain, a date — and only the domain;
        // the address was operational and is not retained (§9.3's spirit).
        assertThat(jdbc.queryForObject("""
                select count(*) from work_verification w
                join member m on m.id = w.member_id
                where m.email = 'co-wv-flow@example.org'
                  and w.company_id = %d and w.domain = 'harbourlight-wv.example'
                """.formatted(company), Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select count(*) from work_link where domain = 'harbourlight-wv.example' "
                        + "and address is not null", Long.class)).isZero();
    }

    @Test
    void aLinkWorksOnceAndExpires() throws Exception {
        Cookie session = memberAt("co-wv-once@example.org", "Wanda Once", "Oncely WV");
        long company = companies.named("Oncely WV").id();
        mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                        .param("address", "w@oncely-wv.example"))
                .andExpect(status().is3xxRedirection());
        String token = latestVerifyToken();
        consume(token, company);
        // Second use: dead.
        mvc.perform(post("/verify").param("token", token))
                .andExpect(status().isOk()); // the link-invalid page, not a redirect

        // A fresh link left to rot past its lifetime is dead too.
        mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                        .param("address", "w2@oncely-wv.example"))
                .andExpect(status().is3xxRedirection());
        String stale = latestVerifyToken();
        clock.advance(java.time.Duration.ofHours(1));
        mvc.perform(post("/verify").param("token", stale)).andExpect(status().isOk());
    }

    @Test
    void anAddressMustLookLikeMailAtARealDomain() throws Exception {
        Cookie session = memberAt("co-wv-bad@example.org", "Malin Formed", "Formless WV");
        long company = companies.named("Formless WV").id();
        int mailsAfterSignup = greenMail.getReceivedMessages().length;

        for (String bad : new String[] {"not-an-email", "someone@nodot", "a b@x.example"}) {
            mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                            .param("address", bad))
                    .andExpect(redirectedUrl("/companies/" + company + "?verification=invalid"));
        }
        assertThat(greenMail.getReceivedMessages()).hasSize(mailsAfterSignup);
    }

    @Test
    void onlyAMemberWithACurrentPositionThereMayAskForALink() throws Exception {
        memberAt("co-wv-owner@example.org", "Occu Pant", "Gatehouse WV");
        long company = companies.named("Gatehouse WV").id();

        // Signed out: to the sign-in page.
        mvc.perform(post("/companies/" + company + "/verify")
                        .param("address", "x@gatehouse-wv.example"))
                .andExpect(redirectedUrl("/login"));

        // A member with no Position there: refused — otherwise anyone receiving mail
        // at any domain could graft that domain onto any Company and force merges.
        Cookie stranger = signUpAndIn("co-wv-stranger@example.org");
        mvc.perform(post("/companies/" + company + "/verify").cookie(stranger)
                        .param("address", "x@gatehouse-wv.example"))
                .andExpect(status().isForbidden());

        // A member whose Position there has ended: also refused (§16 currency).
        Cookie past = memberAt("co-wv-past@example.org", "Berta Left", "Gatehouse WV");
        String editPage = mvc.perform(get("/profile/edit").cookie(past))
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("/profile/positions/(\\d+)/end").matcher(editPage);
        if (!m.find()) {
            throw new AssertionError("No end-position form");
        }
        mvc.perform(post("/profile/positions/" + m.group(1) + "/end").cookie(past)
                        .param("endMonth", "2").param("endYear", "2021"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/companies/" + company + "/verify").cookie(past)
                        .param("address", "b@gatehouse-wv.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendsAreRateLimitedPerAddressAndPerMember() throws Exception {
        Cookie session = memberAt("co-wv-limit@example.org", "Ratey Limit", "Meterworks WV");
        long company = companies.named("Meterworks WV").id();

        // Per address: three an hour, the fourth refused with no mail sent.
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                            .param("address", "same@meterworks-wv.example"))
                    .andExpect(redirectedUrl("/companies/" + company + "?verification=sent"));
        }
        int sentSoFar = greenMail.getReceivedMessages().length;
        mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                        .param("address", "same@meterworks-wv.example"))
                .andExpect(redirectedUrl("/companies/" + company + "?verification=limited"));
        assertThat(greenMail.getReceivedMessages()).hasSize(sentSoFar);

        // Per member: five an hour across any addresses, the sixth refused.
        clock.advance(java.time.Duration.ofHours(2));
        for (int i = 1; i <= 5; i++) {
            mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                            .param("address", "a" + i + "@meterworks-wv.example"))
                    .andExpect(redirectedUrl("/companies/" + company + "?verification=sent"));
        }
        mvc.perform(post("/companies/" + company + "/verify").cookie(session)
                        .param("address", "a6@meterworks-wv.example"))
                .andExpect(redirectedUrl("/companies/" + company + "?verification=limited"));
    }
}
