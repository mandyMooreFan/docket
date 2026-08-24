package com.mbeebe.docket.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.Cookie;

import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SPEC.md §3.1 and §9.3: the age-first signup flow and the age fact's minimal form. */
class SignupFlowTests extends IdentityTestBase {

    @Autowired
    MemberRepository members;

    @Autowired
    LinkRequestRepository linkRequests;

    @Test
    void theAgeAskComesFirstAndCollectsNoEmail() throws Exception {
        mvc.perform(get("/join"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("When were you born?")))
                .andExpect(content().string(containsString("name=\"month\"")))
                .andExpect(content().string(containsString("name=\"year\"")))
                .andExpect(content().string(not(containsString("name=\"email\""))));
    }

    @Test
    void anUnder16IsRefusedAndNothingIsStoredServerSide() throws Exception {
        YearMonth fourteen = YearMonth.now(clock).minusYears(14);
        var result = mvc.perform(post("/join")
                        .param("month", String.valueOf(fourteen.getMonthValue()))
                        .param("year", String.valueOf(fourteen.getYear())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("16 and over")))
                .andExpect(content().string(containsString("Nothing you entered was kept")))
                .andReturn();
        Cookie hold = result.getResponse().getCookie(JoinController.AGE_HOLD_COOKIE);
        assertThat(hold).isNotNull();
        assertThat(hold.getMaxAge()).isEqualTo(24 * 60 * 60);
    }

    @Test
    void theRefusalCookieBlocksResubmissionEvenWithAnAdultAnswer() throws Exception {
        Cookie hold = new Cookie(JoinController.AGE_HOLD_COOKIE, "held");
        mvc.perform(get("/join").cookie(hold))
                .andExpect(content().string(containsString("16 and over")));
        YearMonth adult = YearMonth.now(clock).minusYears(30);
        mvc.perform(post("/join").cookie(hold)
                        .param("month", String.valueOf(adult.getMonthValue()))
                        .param("year", String.valueOf(adult.getYear())))
                .andExpect(content().string(containsString("16 and over")));
    }

    @Test
    void anAdultsBirthMonthAndYearEvaporateAtTheAgeScreen() throws Exception {
        YearMonth birth = YearMonth.now(clock).minusYears(40);
        mvc.perform(post("/join")
                        .param("month", String.valueOf(birth.getMonthValue()))
                        .param("year", String.valueOf(birth.getYear())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"ADULT\"")))
                .andExpect(content().string(not(containsString("birthMonth"))))
                .andExpect(content().string(not(containsString("birthYear"))));

        signUpAndIn("adult@example.org");
        Member member = members.findByEmail("adult@example.org").orElseThrow();
        assertThat(member.ageKind()).isEqualTo(Member.AgeKind.ADULT);
        assertThat(member.isMinor()).isFalse();
    }

    @Test
    void aSixteenYearOldsMonthAndYearAreKeptSolelyForTheRollover() throws Exception {
        YearMonth birth = YearMonth.now(clock).minusYears(17);
        mvc.perform(post("/join")
                        .param("month", String.valueOf(birth.getMonthValue()))
                        .param("year", String.valueOf(birth.getYear())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"MINOR\"")))
                .andExpect(content().string(containsString("birthMonth")));

        mvc.perform(post("/join/link")
                        .param("email", "younger@example.org")
                        .param("ageKind", "MINOR")
                        .param("birthMonth", String.valueOf(birth.getMonthValue()))
                        .param("birthYear", String.valueOf(birth.getYear())))
                .andExpect(status().isOk());
        sessionCookieFor(latestMailedToken());

        Member member = members.findByEmail("younger@example.org").orElseThrow();
        assertThat(member.isMinor()).isTrue();
        assertThat(member.birth()).isEqualTo(birth);
    }

    @Test
    void theFloorHoldsEvenAgainstAnEditedForm() throws Exception {
        YearMonth fourteen = YearMonth.now(clock).minusYears(14);
        mvc.perform(post("/join/link")
                        .param("email", "sneaky@example.org")
                        .param("ageKind", "MINOR")
                        .param("birthMonth", String.valueOf(fourteen.getMonthValue()))
                        .param("birthYear", String.valueOf(fourteen.getYear())))
                .andExpect(content().string(containsString("16 and over")));
        assertThat(linkRequests.countByEmailAndCreatedAtAfter("sneaky@example.org", Instant.EPOCH))
                .isZero();
        assertThat(members.findByEmail("sneaky@example.org")).isEmpty();
    }

    @Test
    void joiningWithAnAddressThatAlreadyHasAnAccountJustSignsIn() throws Exception {
        signUpAndIn("repeat@example.org");
        String secondToken = requestAdultJoinLink("repeat@example.org");
        sessionCookieFor(secondToken);
        assertThat(members.findByEmail("repeat@example.org")).isPresent();
    }
}
