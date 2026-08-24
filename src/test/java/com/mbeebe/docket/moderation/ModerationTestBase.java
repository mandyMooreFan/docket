package com.mbeebe.docket.moderation;

import com.mbeebe.docket.messaging.MessagingTestBase;
import jakarta.servlet.http.Cookie;

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
 * a change to that decision). The address is set in {@code src/test/resources/
 * application.properties} for the whole run rather than by a property source on this
 * class — see the reasoning there; in short, forking a second Spring context puts a
 * fresh clock in front of a shared rate-limit ledger and nothing ever gets mailed.
 *
 * <p>Signing the owner in is idempotent on purpose — {@code requestJoin} turns a second
 * join for a known address into a sign-in (§3.1), so each test class can ask for the
 * owner without caring which one ran first against the shared container.
 */
abstract class ModerationTestBase extends MessagingTestBase {

    static final String OWNER_EMAIL = "mod-owner@docket.test";

    private static final Pattern REPORT_LINK = Pattern.compile("/moderation/reports/(\\d+)");
    private static final Pattern APPEAL_ACTION = Pattern.compile("/appeals/(\\d+)");
    private static final Pattern POST_URL = Pattern.compile("/posts/(\\d+)");
    private static final Pattern AUTH_LINK = Pattern.compile("/auth/([A-Za-z0-9_-]+)");
    private static final Pattern APPEAL_FORM = Pattern.compile("/moderation/appeals/(\\d+)");

    /**
     * Composing and reading the feed, repeated from {@code FeedTestBase} rather than
     * inherited from it. Moderation is the first suite that needs both branches of the
     * base hierarchy at once — Threads and Job postings from this side, Posts and
     * Replies from the other — and §10.2's reportable list spans them all. Repeating
     * two short helpers is cheaper than re-parenting bases five suites depend on.
     */
    protected long compose(Cookie session, String body) throws Exception {
        clock.advance(java.time.Duration.ofMinutes(1));
        String redirect = mvc.perform(post("/posts").cookie(session).param("body", body))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        Matcher matcher = POST_URL.matcher(redirect);
        if (!matcher.find()) {
            throw new AssertionError("Compose did not land on a post page: " + redirect);
        }
        return Long.parseLong(matcher.group(1));
    }

    protected String feedSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Sign the owner in.
     *
     * <p>Deliberately not {@code signUpAndIn}: that reads the <em>last</em> message
     * GreenMail received, and by the time a test wants the owner it has usually just
     * filed a Report, so the last message is an acknowledgement rather than a sign-in
     * link. Scoping to the owner's own mailbox is the same discipline {@code
     * mailBodiesFor} exists for elsewhere — assertions and lookups are per-mailbox,
     * never "whatever arrived most recently".
     */
    protected Cookie owner() throws Exception {
        mvc.perform(post("/join/link")
                        .param("email", OWNER_EMAIL)
                        .param("ageKind", "ADULT"))
                .andExpect(status().isOk());
        for (String body : mailBodiesFor(OWNER_EMAIL).reversed()) {
            Matcher matcher = AUTH_LINK.matcher(body);
            if (matcher.find()) {
                return sessionCookieFor(matcher.group(1));
            }
        }
        throw new AssertionError("No sign-in link reached " + OWNER_EMAIL);
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

    /**
     * The id of the open Report against one particular item.
     *
     * <p>Deliberately not "the oldest open Report": the Postgres container is shared
     * across the whole run, so the queue carries every other suite's undecided Reports
     * too, and acting on the oldest would quietly apply a rung to a member from another
     * test. Scoping to the item is the same rule the rest of the suite follows for
     * mailboxes and counts — never assert or act on "whatever is first".
     */
    protected long reportIdFor(Cookie ownerSession, String itemHref) throws Exception {
        // The boundary matters: a plain contains() for "/posts/12" also matches
        // "/posts/123", so a low-numbered post would pick up a later test's report and
        // apply a rung to the wrong member. Suites pass in isolation and fail together.
        Pattern href = Pattern.compile(Pattern.quote(itemHref) + "(?![0-9])");
        for (String entry : queueSeenBy(ownerSession).split("<article")) {
            if (!href.matcher(entry).find()) {
                continue;
            }
            Matcher matcher = REPORT_LINK.matcher(entry);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        throw new AssertionError("No open report in the queue against " + itemHref);
    }

    /** The open Report against a Post, by the href the queue renders for it. */
    protected long reportIdForPost(Cookie ownerSession, long postId) throws Exception {
        return reportIdFor(ownerSession, "/posts/" + postId);
    }

    /**
     * The id of the open Appeal carrying particular words, scoped for the same reason
     * {@link #reportIdFor} is: the queue holds every suite's open appeals at once.
     */
    protected long appealIdFor(Cookie ownerSession, String account) throws Exception {
        for (String entry : queueSeenBy(ownerSession).split("<article")) {
            if (!flat(entry).contains(account)) {
                continue;
            }
            Matcher matcher = APPEAL_FORM.matcher(entry);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        throw new AssertionError("No open appeal in the queue saying: " + account);
    }

    /** The action id a standing notice offers to appeal — the member's own view of it. */
    protected long appealableActionId(Cookie member) throws Exception {
        Matcher matcher = APPEAL_ACTION.matcher(standingSeenBy(member));
        if (!matcher.find()) {
            throw new AssertionError("Nothing standing against this member to appeal");
        }
        return Long.parseLong(matcher.group(1));
    }

    /** Lodge the one Appeal, reporting the product's own sentence if it is refused. */
    protected void appeal(Cookie member, long actionId, String account) throws Exception {
        var response = mvc.perform(post("/appeals/" + actionId).cookie(member)
                        .param("account", account))
                .andReturn().getResponse();
        if (response.getStatus() / 100 != 3) {
            throw new AssertionError("Appeal against action %d refused (%d): %s"
                    .formatted(actionId, response.getStatus(),
                            response.getErrorMessage() == null
                                    ? flat(response.getContentAsString())
                                    : response.getErrorMessage()));
        }
    }

    /**
     * A rendered page with its whitespace collapsed, for asserting on sentences.
     *
     * <p>Templates wrap their source lines for readability, so a sentence the reader
     * sees as one line reaches the test with a newline and indentation in the middle of
     * it. Asserting against the flattened text checks what the person reads rather than
     * how the template happens to be laid out — and stops a reflow silently breaking a
     * test that is really about copy.
     */
    protected static String flat(String html) {
        return html.replaceAll("\\s+", " ");
    }

    protected String standingSeenBy(Cookie member) throws Exception {
        return mvc.perform(get("/appeals").cookie(member))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Apply one rung of the ladder.
     *
     * <p>On a refusal this reports the product's own sentence rather than only the
     * status code. A rung refuses for several different reasons — the report is already
     * decided, nobody is answerable for the item, the item has gone — and "expected 3xx
     * but was 422" tells you none of them.
     */
    protected void act(Cookie ownerSession, long reportId, String rung,
                       String... params) throws Exception {
        var request = post("/moderation/reports/" + reportId + "/" + rung).cookie(ownerSession);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        var response = mvc.perform(request).andReturn().getResponse();
        if (response.getStatus() / 100 != 3) {
            throw new AssertionError("Rung '%s' on report %d was refused (%d): %s"
                    .formatted(rung, reportId, response.getStatus(),
                            response.getErrorMessage() == null
                                    ? flat(response.getContentAsString())
                                    : response.getErrorMessage()));
        }
    }
}
