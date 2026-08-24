package com.mbeebe.docket.moderation;

import com.mbeebe.docket.messaging.MessagingTestBase;
import jakarta.servlet.http.Cookie;
import org.springframework.test.context.TestPropertySource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The §10 suite's base. Every moderation test needs the same two things the rest of the
 * product does not: a named owner, and a way to get at the queue they alone can see.
 *
 * <p>The owner is configuration (§10.1: one person, and appointing moderators later is
 * a change to that decision), so the address is fixed here for the whole suite. Signing
 * the owner in is idempotent on purpose — {@code requestJoin} turns a second join for a
 * known address into a sign-in (§3.1), so each test class can ask for the owner without
 * caring which one ran first against the shared container.
 */
@TestPropertySource(properties = "docket.owner-email=mod-owner@docket.test")
abstract class ModerationTestBase extends MessagingTestBase {

    static final String OWNER_EMAIL = "mod-owner@docket.test";

    private static final Pattern REPORT_LINK = Pattern.compile("/moderation/reports/(\\d+)");
    private static final Pattern APPEAL_ACTION = Pattern.compile("/appeals/(\\d+)");

    protected Cookie owner() throws Exception {
        return signUpAndIn(OWNER_EMAIL);
    }

    protected void report(Cookie reporter, String kind, long targetId,
                          String category, String account) throws Exception {
        mvc.perform(post("/report/" + kind + "/" + targetId).cookie(reporter)
                        .param("category", category)
                        .param("account", account))
                .andExpect(status().is3xxRedirection());
    }

    protected String queueSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/moderation").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** The oldest open Report's id, read off the queue the way the owner would. */
    protected long oldestOpenReportId(Cookie ownerSession) throws Exception {
        Matcher matcher = REPORT_LINK.matcher(queueSeenBy(ownerSession));
        if (!matcher.find()) {
            throw new AssertionError("No open report in the queue");
        }
        return Long.parseLong(matcher.group(1));
    }

    /** The action id a standing notice offers to appeal — the member's own view of it. */
    protected long appealableActionId(Cookie member) throws Exception {
        Matcher matcher = APPEAL_ACTION.matcher(standingSeenBy(member));
        if (!matcher.find()) {
            throw new AssertionError("Nothing standing against this member to appeal");
        }
        return Long.parseLong(matcher.group(1));
    }

    protected String standingSeenBy(Cookie member) throws Exception {
        return mvc.perform(get("/appeals").cookie(member))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected void act(Cookie ownerSession, long reportId, String rung,
                       String... params) throws Exception {
        var request = post("/moderation/reports/" + reportId + "/" + rung).cookie(ownerSession);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        mvc.perform(request).andExpect(status().is3xxRedirection());
    }
}
