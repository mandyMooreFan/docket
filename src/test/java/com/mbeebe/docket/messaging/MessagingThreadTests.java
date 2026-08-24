package com.mbeebe.docket.messaging;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §7.1–§7.2 on ADR-0001: exactly one permanent Thread per pair,
 * writing authorised by a Connection, derived at every ask (ADR-0002). §7.4:
 * messaging never emails anyone — not even a first message.
 */
class MessagingThreadTests extends MessagingTestBase {

    @Autowired
    JdbcTemplate jdbc;

    long threadCount(long one, long other) {
        return jdbc.queryForObject(
                "select count(*) from thread where member_a = ? and member_b = ?",
                Long.class, Math.min(one, other), Math.max(one, other));
    }

    @Test
    void connectedMembersShareExactlyOneThreadWhicheverSideWrites() throws Exception {
        Cookie ann = completeMember("msg-ann@example.org", "Ann Marchmont");
        Cookie ben = completeMember("msg-ben@example.org", "Ben Marchmont");
        connect(ann, ben);
        long annId = memberId(ann);
        long benId = memberId(ben);

        send(ann, benId, "Hello Ben — long time.");
        clock.advance(java.time.Duration.ofMinutes(1));
        send(ben, annId, "Ann! Good to hear from you.");

        // Both sides read the same correspondence, serif for the words.
        String annView = threadPage(ann, benId);
        assertThat(annView).contains("Hello Ben — long time.")
                .contains("Ann! Good to hear from you.");
        String benView = threadPage(ben, annId);
        assertThat(benView).contains("Hello Ben — long time.")
                .contains("Ann! Good to hear from you.");

        // ADR-0001: one Thread per pair, EVER — both writes landed in the same row.
        assertThat(threadCount(annId, benId)).isEqualTo(1);
    }

    @Test
    void aStrangerHasNoThreadAndNoWrite() throws Exception {
        Cookie carol = completeMember("msg-carol@example.org", "Carol Strange");
        Cookie dan = completeMember("msg-dan@example.org", "Dan Stranger");
        long carolId = memberId(carol);
        long danId = memberId(dan);

        // §7.1: nothing but a Connection or an open Application opens a channel.
        mvc.perform(multipart("/messages/" + danId).param("body", "Cold call")
                        .cookie(carol))
                .andExpect(status().isForbidden());
        mvc.perform(get("/messages/" + danId).cookie(carol))
                .andExpect(status().isNotFound());
        mvc.perform(get("/messages/" + carolId).cookie(dan))
                .andExpect(status().isNotFound());
        assertThat(threadCount(carolId, danId)).isZero();
    }

    @Test
    void thereIsNoThreadWithYourself() throws Exception {
        Cookie eve = completeMember("msg-eve@example.org", "Eve Solo");
        long eveId = memberId(eve);
        mvc.perform(get("/messages/" + eveId).cookie(eve))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/messages/" + eveId).param("body", "Note to self")
                        .cookie(eve))
                .andExpect(status().isNotFound());
    }

    @Test
    void writingIsTheMessageCapabilityDerivedAtEveryAsk() throws Exception {
        Cookie fay = completeMember("msg-fay@example.org", "Fay Gates");
        Cookie gus = completeMember("msg-gus@example.org", "Gus Gates");
        connect(fay, gus);
        long gusId = memberId(gus);
        send(fay, gusId, "While complete, this works.");

        // §3.2 + ADR-0002: blank the headline and Completeness — and with it the
        // MESSAGE capability — is gone at the very next ask; no flag to forget.
        mvc.perform(post("/profile/basics").cookie(fay)
                        .param("name", "Fay Gates").param("headline", "")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/messages/" + gusId).param("body", "And now?")
                        .cookie(fay))
                .andExpect(status().isForbidden())
                .andExpect(status().reason(containsString("profile is complete")));
        assertThat(threadPage(fay, gusId))
                .contains("Messaging opens when your profile is complete.")
                .doesNotContain("Write a message");

        // Restoring the headline restores the capability — same derivation.
        mvc.perform(post("/profile/basics").cookie(fay)
                        .param("name", "Fay Gates").param("headline", "Back again")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        send(fay, gusId, "Complete again.");
    }

    @Test
    void aMessageIsBoundedEscapedAndLinkified() throws Exception {
        Cookie hal = completeMember("msg-hal@example.org", "Hal Words");
        Cookie ivy = completeMember("msg-ivy@example.org", "Ivy Words");
        connect(hal, ivy);
        long ivyId = memberId(ivy);

        mvc.perform(multipart("/messages/" + ivyId).param("body", "").cookie(hal))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("A message needs words.")));
        mvc.perform(multipart("/messages/" + ivyId).param("body", "x".repeat(8001))
                        .cookie(hal))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("at most 8,000 characters")));

        send(hal, ivyId, "See <script>alert(1)</script> and https://example.com/role");
        String page = threadPage(ivy, memberId(hal));
        assertThat(page).contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>alert(1)")
                .contains("<a href=\"https://example.com/role\" rel=\"nofollow noopener\">");
    }

    @Test
    void messagingNeverSendsMailNotEvenAFirstMessage() throws Exception {
        Cookie jon = completeMember("msg-jon@example.org", "Jon Quiet");
        Cookie kim = completeMember("msg-kim@example.org", "Kim Quiet");
        connect(jon, kim);
        int kimMailBefore = mailBodiesFor("msg-kim@example.org").size();

        send(jon, memberId(kim), "A first message arrives only in the inbox.");
        clock.advance(java.time.Duration.ofMinutes(1));
        send(jon, memberId(kim), "And a second.");

        // §7.4: no email at all — the mailbox-scoped count is untouched.
        assertThat(mailBodiesFor("msg-kim@example.org")).hasSize(kimMailBefore);
    }
}
