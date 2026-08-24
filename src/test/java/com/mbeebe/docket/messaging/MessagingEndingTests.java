package com.mbeebe.docket.messaging;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §7.3, the endings. Disconnect closes the Thread to new Messages and
 * both sides keep the history; reconnecting reopens the very same Thread. Block
 * is total — and, because §4.2 will not let a decline be observable, a Block
 * must look to the blocked party exactly like a Disconnect.
 */
class MessagingEndingTests extends MessagingTestBase {

    @Autowired
    JdbcTemplate jdbc;

    long threadCount(long one, long other) {
        return jdbc.queryForObject(
                "select count(*) from thread where member_a = ? and member_b = ?",
                Long.class, Math.min(one, other), Math.max(one, other));
    }

    long messageCount(long one, long other) {
        return jdbc.queryForObject(
                "select count(*) from message m join thread t on m.thread_id = t.id "
                        + "where t.member_a = ? and t.member_b = ?",
                Long.class, Math.min(one, other), Math.max(one, other));
    }

    @Test
    void disconnectClosesTheThreadBothWaysAndKeepsEveryWord() throws Exception {
        Cookie nell = completeMember("msg-nell@example.org", "Nell Parted");
        Cookie omar = completeMember("msg-omar@example.org", "Omar Parted");
        connect(nell, omar);
        long nellId = memberId(nell);
        long omarId = memberId(omar);
        send(nell, omarId, "Worth keeping, this one.");
        clock.advance(Duration.ofMinutes(1));
        send(omar, nellId, "Agreed entirely.");

        disconnect(nell, omarId);

        // §7.3: closed to new Messages — in both directions, derived at the ask.
        mvc.perform(multipart("/messages/" + omarId).param("body", "Once more")
                        .cookie(nell))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/messages/" + nellId).param("body", "Once more")
                        .cookie(omar))
                .andExpect(status().isForbidden());
        assertThat(messageCount(nellId, omarId)).isEqualTo(2);

        // §7.3 + §11.1: both sides keep the history, and the composer is replaced
        // by one honest sentence rather than vanishing without explanation.
        for (String page : new String[] {threadPage(nell, omarId), threadPage(omar, nellId)}) {
            assertThat(page).contains("Worth keeping, this one.")
                    .contains("Agreed entirely.")
                    .contains("This thread is closed to new messages.")
                    .doesNotContain("Write a message");
        }
    }

    @Test
    void reconnectingReopensTheVerySameThread() throws Exception {
        Cookie pia = completeMember("msg-pia@example.org", "Pia Return");
        Cookie quinn = completeMember("msg-quinn@example.org", "Quinn Return");
        connect(pia, quinn);
        long piaId = memberId(pia);
        long quinnId = memberId(quinn);
        send(pia, quinnId, "Before the break.");

        disconnect(pia, quinnId);
        connect(quinn, pia);

        // ADR-0001: one Thread per pair, EVER — nothing was created a second time.
        assertThat(threadCount(piaId, quinnId)).isEqualTo(1);
        clock.advance(Duration.ofMinutes(1));
        send(pia, quinnId, "After the break.");
        assertThat(threadCount(piaId, quinnId)).isEqualTo(1);
        assertThat(threadPage(quinn, piaId))
                .contains("Before the break.")
                .contains("After the break.")
                .contains("Write a message");
    }

    @Test
    void blockStopsMessagesBothWaysAndStillKeepsTheHistory() throws Exception {
        Cookie rosa = completeMember("msg-rosa@example.org", "Rosa Severed");
        Cookie sam = completeMember("msg-sam@example.org", "Sam Severed");
        connect(rosa, sam);
        long rosaId = memberId(rosa);
        long samId = memberId(sam);
        send(rosa, samId, "Said before the block.");

        block(rosa, samId);

        // §7.3: no Messages either direction — including from the blocker.
        mvc.perform(multipart("/messages/" + samId).param("body", "More")
                        .cookie(rosa))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/messages/" + rosaId).param("body", "More")
                        .cookie(sam))
                .andExpect(status().isForbidden());
        assertThat(messageCount(rosaId, samId)).isEqualTo(1);

        // §11.1: neither person may destroy the other's record of what was said.
        assertThat(threadPage(rosa, samId)).contains("Said before the block.");
        assertThat(threadPage(sam, rosaId)).contains("Said before the block.");
    }

    @Test
    void aBlockIsIndistinguishableFromADisconnectToThePersonItLandsOn() throws Exception {
        Cookie tess = completeMember("msg-tess@example.org", "Tess Quiet");
        Cookie ugo = completeMember("msg-ugo@example.org", "Ugo Dropped");
        Cookie vera = completeMember("msg-vera@example.org", "Vera Quiet");
        Cookie wes = completeMember("msg-wes@example.org", "Wes Blocked");
        connect(tess, ugo);
        connect(vera, wes);
        long tessId = memberId(tess);
        long ugoId = memberId(ugo);
        long veraId = memberId(vera);
        long wesId = memberId(wes);
        send(tess, ugoId, "The same words either way.");
        send(vera, wesId, "The same words either way.");

        disconnect(tess, ugoId);
        block(vera, wesId);

        // The refusal is one sentence, and it is the same sentence.
        assertThat(refusalOfWriting(wes, veraId)).isEqualTo(refusalOfWriting(ugo, tessId));

        // And so is the page. Setting aside the reader's own identity and the row
        // ids — which say nothing about how the Thread closed — the two renderings
        // are the same bytes: same shape, same wording, nothing naming a Block.
        String dropped = anonymised(threadPage(ugo, tessId), "Tess Quiet", "msg-ugo");
        String blocked = anonymised(threadPage(wes, veraId), "Vera Quiet", "msg-wes");
        assertThat(blocked).isEqualTo(dropped)
                .doesNotContain("block").doesNotContain("Block");
    }

    /** Everything that differs because of WHO is reading, and nothing else. */
    private static String anonymised(String page, String otherName, String ownEmail) {
        return page.replace(otherName, "NAME")
                .replace(ownEmail + "@example.org", "self@example.org")
                .replaceAll("/(messages|p)/\\d+", "/x/N")
                .replaceAll("message-\\d+", "message-N")
                // §10.2 put a report link on every Message. Its row id says no more
                // about how the Thread closed than the anchor id beside it does.
                .replaceAll("/report/MESSAGE/\\d+", "/report/MESSAGE/N");
    }
}
