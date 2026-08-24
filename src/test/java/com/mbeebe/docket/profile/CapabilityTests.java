package com.mbeebe.docket.profile;

import com.mbeebe.docket.DocketTestBase;
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
 * SPEC.md §3.2 and ADR-0002: Capability is a conclusion drawn at read time from
 * stored facts — never held, never granted, never persisted.
 */
class CapabilityTests extends DocketTestBase {

    @Autowired
    CapabilityService capabilities;

    @Autowired
    JdbcTemplate jdbc;

    long memberId(Cookie session) throws Exception {
        String url = mvc.perform(get("/profile").cookie(session))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

    @Test
    void aFreshMemberCanReadEverythingButDoNothingToAnyone() throws Exception {
        long member = memberId(signUpAndIn("lurker@example.org"));
        for (Capability capability : Capability.values()) {
            if (!capability.earnedByCompleteness()) {
                continue;
            }
            assertThat(capabilities.may(member, capability))
                    .as("a fresh account holds no %s", capability)
                    .isEqualTo(CapabilityAnswer.NOT_YET_EARNED);
        }
    }

    /**
     * §3.2's withheld list does not include replying, and #38 made that explicit rather
     * than changing it: {@code PostService.reply} never asked for a Capability, because
     * "the Connection itself was the earned thing". REPLY exists as a Capability only
     * so §10.3 can withdraw it — it names Replies among the four things a Withdrawal
     * can take — so it is the one Capability that is never NOT_YET_EARNED.
     */
    @Test
    void replyingWasNeverWithheldByCompletenessAndStillIsNot() throws Exception {
        long member = memberId(signUpAndIn("lurker-reply@example.org"));

        assertThat(capabilities.may(member, Capability.REPLY))
                .isEqualTo(CapabilityAnswer.YES);
        assertThat(Capability.REPLY.earnedByCompleteness()).isFalse();
    }

    @Test
    void completingTheProfileEarnsEveryCapabilityAtOnce() throws Exception {
        Cookie session = signUpAndIn("earner@example.org");
        long member = memberId(session);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", "Earner").param("headline", "Has a headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        assertThat(capabilities.may(member, Capability.POST))
                .isEqualTo(CapabilityAnswer.NOT_YET_EARNED);

        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", "")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());

        for (Capability capability : Capability.values()) {
            assertThat(capabilities.may(member, capability))
                    .as("a complete profile earns %s", capability)
                    .isEqualTo(CapabilityAnswer.YES);
        }
    }

    @Test
    void noTableStoresAConclusionARuleCouldCompute() {
        // ADR-0002's tripwire: columns that smell like stored capability, visibility
        // or completeness may not exist anywhere in the schema.
        List<String> suspicious = jdbc.queryForList("""
                select table_name || '.' || column_name
                from information_schema.columns
                where table_schema = 'public'
                  and (column_name like 'can\\_%'
                       or column_name like '%\\_allowed'
                       or column_name in ('complete', 'completeness', 'capability',
                                          'indexable', 'visible', 'effective_visibility',
                                          'searchable'))
                  -- moderation_action.capability is the one column allowed to carry the
                  -- word, and it is the subject of a fact rather than a conclusion: it
                  -- records WHICH Capability a §10.3 Withdrawal took, not whether some
                  -- Member holds one. The conclusion is still derived at every ask, by
                  -- CapabilityService reading these rows — which is exactly what the
                  -- tests above prove. Deliberately narrow: any other table growing a
                  -- 'capability' column still trips this.
                  and table_name || '.' || column_name <> 'moderation_action.capability'
                """, String.class);
        assertThat(suspicious).isEmpty();
    }
}
