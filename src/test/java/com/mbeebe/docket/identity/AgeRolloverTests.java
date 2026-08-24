package com.mbeebe.docket.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** §9.3: the protections lift at the end of the birth month they turn 18 — automatically. */
class AgeRolloverTests extends IdentityTestBase {

    @Autowired
    AgeRollover rollover;

    @Autowired
    MemberRepository members;

    private Member minorBorn(YearMonth birth, String email) throws Exception {
        mvc.perform(post("/join/link")
                        .param("email", email)
                        .param("ageKind", "MINOR")
                        .param("birthMonth", String.valueOf(birth.getMonthValue()))
                        .param("birthYear", String.valueOf(birth.getYear())))
                .andExpect(status().isOk());
        sessionCookieFor(latestMailedToken());
        return members.findByEmail(email).orElseThrow();
    }

    @Test
    void aMinorCollapsesToTheAdultFactAfterTheirEighteenthBirthMonth() throws Exception {
        YearMonth birth = YearMonth.now(clock).minusYears(17);
        minorBorn(birth, "almost@example.org");

        // Still inside the birth month they turn 18: nothing happens to THIS member.
        // (Assertions are member-scoped, not counts: the container is shared, and
        // other suites' minors may be due in windows this test doesn't control.)
        rollover.rolloverDueMinors(birth.plusYears(18));
        assertThat(members.findByEmail("almost@example.org").orElseThrow().isMinor()).isTrue();

        // The month after: the fact collapses and the birth data is gone.
        rollover.rolloverDueMinors(birth.plusYears(18).plusMonths(1));
        Member rolled = members.findByEmail("almost@example.org").orElseThrow();
        assertThat(rolled.isMinor()).isFalse();
        assertThat(rolled.ageKind()).isEqualTo(Member.AgeKind.ADULT);
    }
}
