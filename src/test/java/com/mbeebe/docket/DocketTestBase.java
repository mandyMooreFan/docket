package com.mbeebe.docket;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DocketTestBase.TestClockConfig.class)
public abstract class DocketTestBase {

    @TestConfiguration
    public static class TestClockConfig {
        @Bean
        @Primary
        SteppingClock steppingClock() {
            return new SteppingClock();
        }
    }

    // Singleton container, started once for every test class: the JUnit
    // @Container lifecycle would stop it after the first subclass finishes, killing
    // the cached Spring context's connection pool.
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    static {
        postgres.start();
    }

    // A free port, chosen once per JVM. ServerSetupTest.SMTP is the fixed 3025, which
    // is machine-wide: two test runs in two worktrees cannot both hold it, and the
    // loser fails every class with "Could not start mail server" — indistinguishable
    // from a broken branch unless you go looking at ss(8). The port is resolved here,
    // in a static initialiser, rather than read back off the extension: JUnit starts
    // SpringExtension before a @RegisterExtension field, so a supplier asking GreenMail
    // for its port could be evaluated before GreenMail has one.
    private static final int SMTP_PORT = freePort();

    private static int freePort() {
        try (java.net.ServerSocket probe = new java.net.ServerSocket(0)) {
            return probe.getLocalPort();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No free port for the SMTP fake", e);
        }
    }

    @RegisterExtension
    protected static GreenMailExtension greenMail = new GreenMailExtension(
            new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP))
            .withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> SMTP_PORT);
    }

    private static final Pattern LINK = Pattern.compile("/auth/([A-Za-z0-9_-]+)");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected SteppingClock clock;

    @BeforeEach
    void isolateFromEarlierTests() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        // Two hours forward: every test starts with fresh rate-limit windows.
        clock.advance(Duration.ofHours(2));
    }

    protected String requestAdultJoinLink(String email) throws Exception {
        mvc.perform(post("/join/link")
                        .param("email", email)
                        .param("ageKind", "ADULT"))
                .andExpect(status().isOk());
        return latestMailedToken();
    }

    protected String latestMailedToken() {
        var messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("No auth link in mail body:\n" + body);
        }
        return matcher.group(1);
    }

    protected Cookie sessionCookieFor(String token) throws Exception {
        MvcResult result = mvc.perform(post("/auth").param("token", token))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("docket_session");
        if (cookie == null) {
            throw new AssertionError("No session cookie after consuming link");
        }
        return cookie;
    }

    protected Cookie signUpAndIn(String email) throws Exception {
        return sessionCookieFor(requestAdultJoinLink(email));
    }

    /**
     * Signs up a 16½-year-old: the §9 floors need a minor to exercise them. Deliberately
     * younger than AgeRolloverTests' 17-year-old — the container is shared, and a minor
     * born in that same month would be swept into its rollover window.
     */
    protected Cookie signUpMinorAndIn(String email) throws Exception {
        YearMonth birth = YearMonth.now(clock).minusYears(17).plusMonths(6);
        mvc.perform(post("/join/link")
                        .param("email", email)
                        .param("ageKind", "MINOR")
                        .param("birthMonth", String.valueOf(birth.getMonthValue()))
                        .param("birthYear", String.valueOf(birth.getYear())))
                .andExpect(status().isOk());
        return sessionCookieFor(latestMailedToken());
    }
}
