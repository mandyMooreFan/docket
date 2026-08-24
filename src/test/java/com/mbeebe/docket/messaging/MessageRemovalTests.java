package com.mbeebe.docket.messaging;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.moderation.TargetKind;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.3 rung 1 inside a private Thread. A Message is immutable to both
 * correspondents (§7.3, §11.1) — moderation is not a correspondent, and illegal
 * content in a private Thread is still illegal content, so removal reaches here
 * and takes the Message out of the Thread, the inbox preview and §7.4's count.
 *
 * <p>The Postgres container and GreenMail are shared across the whole run, so
 * every email here is prefixed "rm-msg-" and every assertion is member-scoped —
 * the badge is asserted as the layout writes it, never as a global count.
 */
class MessageRemovalTests extends MessagingTestBase {

    private static final Pattern IMAGE_URL = Pattern.compile("/images/(\\d+)");

    @Autowired
    MessageReportable removals;

    @Autowired
    MessageRepository messages;

    @Autowired
    MessageThreadRepository threads;

    @Autowired
    Members members;

    /** The rendered app bar's badge, exactly as the layout writes it. */
    private static String badge(int count) {
        return "Messages<span class=\"badge\">" + count + "</span>";
    }

    /** The newest Message in the pair's Thread — the one the test just sent. */
    private long latestMessageBetween(long one, long other) {
        long threadId = threads
                .findByMemberAAndMemberB(Math.min(one, other), Math.max(one, other))
                .orElseThrow().id();
        return messages.findFirstByThreadIdAndRemovedAtIsNullOrderByIdDesc(threadId)
                .orElseThrow().id();
    }

    private void remove(long messageId) {
        clock.advance(Duration.ofMinutes(1));
        assertThat(removals.remove(TargetKind.MESSAGE, messageId, clock.instant())).isTrue();
    }

    @Test
    void aRemovedMessageLeavesTheThreadTheInboxPreviewAndTheUnreadCount() throws Exception {
        Cookie writer = completeMember("rm-msg-w1@example.org", "Wanda Writer");
        Cookie reader = completeMember("rm-msg-r1@example.org", "Reada Recipient");
        connect(writer, reader);
        long writerId = memberId(writer);
        long readerId = memberId(reader);
        send(writer, readerId, "The first thing said, which stands.");
        clock.advance(Duration.ofMinutes(1));
        send(writer, readerId, "The sentence that was reported.");
        long reportedId = latestMessageBetween(writerId, readerId);

        assertThat(inboxPage(reader)).contains("The sentence that was reported.");
        assertThat(inboxPage(reader)).contains(badge(2));

        remove(reportedId);

        String inbox = inboxPage(reader);
        assertThat(inbox).doesNotContain("The sentence that was reported.");
        // The preview falls back to the latest Message that still stands (§7.2).
        assertThat(inbox).contains("The first thing said, which stands.");
        // §7.4's count is derived, so it cannot point at something the Thread
        // will not show — two unread became one, not a stale two.
        assertThat(inbox).contains(badge(1));

        String thread = threadPage(reader, writerId);
        assertThat(thread).doesNotContain("The sentence that was reported.");
        assertThat(thread).contains("The first thing said, which stands.");
        // Removal reaches both correspondents; its author has no private copy.
        assertThat(threadPage(writer, readerId))
                .doesNotContain("The sentence that was reported.");
    }

    @Test
    void aRemovedMessageTakesItsImagesWithItForBothCorrespondents() throws Exception {
        Cookie writer = completeMember("rm-msg-w2@example.org", "Imagea Writer");
        Cookie reader = completeMember("rm-msg-r2@example.org", "Imagea Recipient");
        connect(writer, reader);
        long writerId = memberId(writer);
        long readerId = memberId(reader);
        byte[] png = "removed-message-bytes".getBytes(StandardCharsets.UTF_8);
        mvc.perform(multipart("/messages/" + readerId)
                        .file(new MockMultipartFile("images", "shot.png", "image/png", png))
                        .param("body", "A picture said in private.")
                        .cookie(writer))
                .andExpect(status().is3xxRedirection());
        long messageId = latestMessageBetween(writerId, readerId);
        Matcher matcher = IMAGE_URL.matcher(threadPage(reader, writerId));
        assertThat(matcher.find()).isTrue();
        long imageId = Long.parseLong(matcher.group(1));
        mvc.perform(get("/images/" + imageId).cookie(reader)).andExpect(status().isOk());

        remove(messageId);

        mvc.perform(get("/images/" + imageId).cookie(reader)).andExpect(status().isNotFound());
        mvc.perform(get("/images/" + imageId).cookie(writer)).andExpect(status().isNotFound());
    }

    @Test
    void aRestoredMessageIsBackInTheThreadTheInboxAndTheCount() throws Exception {
        Cookie writer = completeMember("rm-msg-w3@example.org", "Backa Writer");
        Cookie reader = completeMember("rm-msg-r3@example.org", "Backa Recipient");
        connect(writer, reader);
        long writerId = memberId(writer);
        long readerId = memberId(reader);
        send(writer, readerId, "Held while the queue looked at it.");
        long messageId = latestMessageBetween(writerId, readerId);

        remove(messageId);
        assertThat(inboxPage(reader)).doesNotContain("Held while the queue looked at it.");

        assertThat(removals.restore(TargetKind.MESSAGE, messageId)).isTrue();

        assertThat(inboxPage(reader)).contains("Held while the queue looked at it.");
        assertThat(inboxPage(reader)).contains(badge(1));
        assertThat(threadPage(reader, writerId)).contains("Held while the queue looked at it.");
    }

    @Test
    void onlyAParticipantMayReportAMessageThoughTheQueueAlwaysReadsIt() throws Exception {
        Cookie writer = completeMember("rm-msg-w4@example.org", "Privy Writer");
        Cookie reader = completeMember("rm-msg-r4@example.org", "Privy Recipient");
        Cookie outsider = completeMember("rm-msg-o4@example.org", "Nosy Outsider");
        connect(writer, reader);
        long writerId = memberId(writer);
        long readerId = memberId(reader);
        send(writer, readerId, "Said to one person and to nobody else.");
        long messageId = latestMessageBetween(writerId, readerId);

        assertThat(removals.visibleToReporter(TargetKind.MESSAGE, messageId,
                memberOf(reader))).isPresent();
        // §10.2's hard line: private is private by construction. An outsider who
        // guesses the id gets the same empty answer as for a Message never written.
        assertThat(removals.visibleToReporter(TargetKind.MESSAGE, messageId,
                memberOf(outsider))).isEmpty();
        assertThat(removals.visibleToReporter(TargetKind.MESSAGE, messageId,
                Optional.empty())).isEmpty();
        // The queue must be able to read what was reported, or it cannot judge it.
        assertThat(removals.forModeration(TargetKind.MESSAGE, messageId))
                .get()
                .satisfies(item -> {
                    assertThat(item.summary()).isEqualTo("Said to one person and to nobody else.");
                    assertThat(item.authorId()).contains(writerId);
                    assertThat(item.href()).isEqualTo("/messages/" + writerId);
                });
    }

    @Test
    void messagingAnswersForMessagesAndForNoOtherKind() throws Exception {
        Cookie writer = completeMember("rm-msg-w5@example.org", "Ownkind Writer");
        Cookie reader = completeMember("rm-msg-r5@example.org", "Ownkind Recipient");
        connect(writer, reader);
        long messageIdSource = memberId(reader);
        send(writer, messageIdSource, "Owned by the messaging module.");
        long messageId = latestMessageBetween(memberId(writer), messageIdSource);

        assertThat(removals.forModeration(TargetKind.MESSAGE, messageId)).isPresent();
        assertThat(removals.forModeration(TargetKind.POST, messageId)).isEmpty();
        assertThat(removals.forModeration(TargetKind.PROFILE, messageId)).isEmpty();
        assertThat(removals.remove(TargetKind.REPLY, messageId, clock.instant())).isFalse();
        assertThat(removals.restore(TargetKind.COMPANY, messageId)).isFalse();
        // A row that does not exist is empty even for the queue.
        assertThat(removals.forModeration(TargetKind.MESSAGE, 987_654_321L)).isEmpty();
    }

    /** The Member behind a session, as the contributor's viewer argument wants it. */
    private Optional<Member> memberOf(Cookie session) throws Exception {
        return members.find(memberId(session));
    }
}
