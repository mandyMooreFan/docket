package com.mbeebe.docket.invites;

import com.mbeebe.docket.jobs.JobsTestBase;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invite-suite plumbing (§13.3). The Postgres container and GreenMail are shared
 * across every suite in the run, so every address here is prefixed "inv-", every
 * mail assertion is fetched by recipient rather than off the end of one global
 * list, and every graph assertion is member-scoped.
 *
 * <p>The join helpers here exist because {@code DocketTestBase.latestMailedToken}
 * reads the newest message in the whole run, and an Invite in flight is very
 * often that message. These read the magic link out of one mailbox instead.
 */
public abstract class InviteTestBase extends JobsTestBase {

    private static final Pattern AUTH_LINK = Pattern.compile("/auth/([A-Za-z0-9_-]+)");

    protected ResultActions invite(Cookie sender, String email, String note) throws Exception {
        return mvc.perform(post("/invite").cookie(sender)
                .param("email", email).param("note", note));
    }

    /** An accepted send: one redirect, and the same one whatever the far end holds. */
    protected void inviteAccepted(Cookie sender, String email, String note) throws Exception {
        invite(sender, email, note)
                .andExpect(status().is3xxRedirection());
    }

    protected String invitePage(Cookie viewer) throws Exception {
        return mvc.perform(get("/invite").cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** The ordinary §3.1 adult signup, reading its link out of this mailbox only. */
    protected Cookie joinAdultAt(String email) throws Exception {
        mvc.perform(post("/join/link").param("email", email).param("ageKind", "ADULT"))
                .andExpect(status().isOk());
        return sessionCookieFor(magicLinkTokenFor(email));
    }

    /** The same, for the 16½-year-old of {@code DocketTestBase} (§9's floor). */
    protected Cookie joinMinorAt(String email) throws Exception {
        var birth = java.time.YearMonth.now(clock).minusYears(17).plusMonths(6);
        mvc.perform(post("/join/link").param("email", email).param("ageKind", "MINOR")
                        .param("birthMonth", String.valueOf(birth.getMonthValue()))
                        .param("birthYear", String.valueOf(birth.getYear())))
                .andExpect(status().isOk());
        return sessionCookieFor(magicLinkTokenFor(email));
    }

    protected String magicLinkTokenFor(String recipient) throws Exception {
        List<String> bodies = mailBodiesFor(recipient);
        for (int i = bodies.size() - 1; i >= 0; i--) {
            Matcher matcher = AUTH_LINK.matcher(bodies.get(i));
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new AssertionError("No magic link among " + bodies.size()
                + " message(s) for " + recipient);
    }

    protected String networkPage(Cookie viewer) throws Exception {
        return mvc.perform(get("/network").cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Makes a member complete enough to hold INVITE, exactly as CONNECT is earned. */
    protected Cookie inviter(String email, String name) throws Exception {
        return completeMember(email, name);
    }

    /**
     * §3.2's bar, applied to a session that already exists — the graph's own
     * helper starts from a fresh signup, and these suites need to complete
     * somebody who joined at a specific invited address.
     */
    protected Cookie completeProfileOf(Cookie session, String name) throws Exception {
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", "")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }
}
