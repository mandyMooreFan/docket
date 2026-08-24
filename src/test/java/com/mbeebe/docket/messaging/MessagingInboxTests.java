package com.mbeebe.docket.messaging;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §7.2's inbox — "a list of people" — §13.4's empty copy, and §7.4's
 * Unread count: the only badge in the product, derived at every ask, absent at
 * zero, and carrying no information back to the person who wrote to you.
 */
class MessagingInboxTests extends MessagingTestBase {

    private static final String EMPTY_INBOX =
            "Messages open when you're connected to someone, or when someone applies to "
                    + "your posting. No connections yet — your inbox is waiting on the graph, "
                    + "not on you.";

    /** The rendered app bar's badge, exactly as the layout writes it. */
    private static String badge(int count) {
        return "Messages<span class=\"badge\">" + count + "</span>";
    }

    @Test
    void theEmptyInboxSaysWhatItIsInTheSpecsOwnWords() throws Exception {
        Cookie abe = completeMember("msg-abe@example.org", "Abe Alone");
        assertThat(inboxPage(abe).replaceAll("\\s+", " ")).contains(EMPTY_INBOX);
    }

    @Test
    void theInboxIsAListOfPeopleNewestCorrespondenceFirst() throws Exception {
        Cookie bea = completeMember("msg-bea@example.org", "Bea Busy");
        Cookie cal = completeMember("msg-cal@example.org", "Cal Early");
        Cookie dot = completeMember("msg-dot@example.org", "Dot Later");
        connect(bea, cal);
        connect(bea, dot);
        send(bea, memberId(cal), "The older correspondence.");
        clock.advance(Duration.ofHours(1));
        send(dot, memberId(bea), "The newer correspondence.");

        String inbox = inboxPage(bea);
        assertThat(inbox).doesNotContain(EMPTY_INBOX)
                .contains("Dot Later").contains("Cal Early")
                .contains("The newer correspondence.")
                .contains("The older correspondence.");
        // Ordered by the latest thing said, and by nothing else — no ranking (§5.1's rule
        // holds everywhere): the person spoken to most recently is at the top.
        assertThat(inbox.indexOf("Dot Later")).isLessThan(inbox.indexOf("Cal Early"));
    }

    @Test
    void aThreadClosedToWritingStaysListedAndReadable() throws Exception {
        Cookie eli = completeMember("msg-eli@example.org", "Eli Kept");
        Cookie fern = completeMember("msg-fern@example.org", "Fern Kept");
        connect(eli, fern);
        long fernId = memberId(fern);
        send(eli, fernId, "Still in the inbox afterwards.");
        disconnect(eli, fernId);

        assertThat(inboxPage(eli)).contains("Fern Kept")
                .contains("Still in the inbox afterwards.")
                .doesNotContain(EMPTY_INBOX);
    }

    @Test
    void theUnreadCountIsTheOneBadgeAndThereIsNothingAtZero() throws Exception {
        Cookie gil = completeMember("msg-gil@example.org", "Gil Counting");
        Cookie hana = completeMember("msg-hana@example.org", "Hana Counting");
        connect(gil, hana);
        long gilId = memberId(gil);
        long hanaId = memberId(hana);

        // §5.6: no badges anywhere — including here, before anyone has written.
        assertThat(feedSeenByMember(hana)).doesNotContain("class=\"badge\"");

        send(gil, hanaId, "One.");
        clock.advance(Duration.ofMinutes(1));
        send(gil, hanaId, "Two.");

        // §7.4: earned by a person writing to you personally. It rides the Messages
        // nav item and nothing else — one badge on the whole page.
        String feed = feedSeenByMember(hana);
        assertThat(feed).contains(badge(2));
        assertThat(feed.split("class=\"badge\"", -1)).hasSize(2);

        // Your own words are never awaiting you.
        assertThat(feedSeenByMember(gil)).doesNotContain("class=\"badge\"");

        // Reading the Thread clears it, and derives to nothing rather than to zero.
        threadPage(hana, gilId);
        assertThat(feedSeenByMember(hana)).doesNotContain("class=\"badge\"");

        // A new Message counts again from the mark, not from the beginning.
        clock.advance(Duration.ofMinutes(1));
        send(gil, hanaId, "Three.");
        assertThat(feedSeenByMember(hana)).contains(badge(1));
    }

    @Test
    void nothingInTheOtherSidesRenderingChangesWhenYouRead() throws Exception {
        Cookie ida = completeMember("msg-ida@example.org", "Ida Private");
        Cookie jed = completeMember("msg-jed@example.org", "Jed Private");
        connect(ida, jed);
        long idaId = memberId(ida);
        long jedId = memberId(jed);
        send(ida, jedId, "Something to read.");
        clock.advance(Duration.ofMinutes(1));
        send(jed, idaId, "Something to be read.");

        // Jed settles his own side first, so his own badge is not what moves.
        threadPage(jed, idaId);
        String before = threadPage(jed, idaId);

        // Ida reads Jed's Message. §7.2: no read receipts, ever.
        threadPage(ida, jedId);

        assertThat(threadPage(jed, idaId)).isEqualTo(before);
        assertThat(before).doesNotContain("Read").doesNotContain("Seen")
                .doesNotContain("Delivered").doesNotContain("Online");
    }

    @Test
    void anOpenThreadRefreshesByPollingAndNothingElse() throws Exception {
        Cookie kit = completeMember("msg-kit@example.org", "Kit Polling");
        Cookie lou = completeMember("msg-lou@example.org", "Lou Polling");
        connect(kit, lou);
        long louId = memberId(lou);
        send(kit, louId, "Arrives on the next poll.");

        // §7.2 + §14.2: one htmx polling attribute, request/response, no socket.
        String page = threadPage(kit, louId);
        assertThat(page).contains("hx-get=\"/messages/" + louId + "/list\"")
                .contains("hx-trigger=\"every 10s\"")
                .doesNotContain("WebSocket").doesNotContain("sse-").doesNotContain("EventSource");

        // The polled fragment is the message list — a fragment, not a second page.
        String fragment = mvc.perform(get("/messages/" + louId + "/list").cookie(kit))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(fragment).contains("Arrives on the next poll.")
                .doesNotContain("<html").doesNotContain("appbar");

        // And it is as private as the page: a stranger polls nothing.
        Cookie mae = completeMember("msg-mae@example.org", "Mae Elsewhere");
        mvc.perform(get("/messages/" + louId + "/list").cookie(mae))
                .andExpect(status().isNotFound());
    }

    /** Any page carrying the app bar will do; the feed is the one everybody lands on. */
    private String feedSeenByMember(Cookie session) throws Exception {
        return mvc.perform(get("/").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
