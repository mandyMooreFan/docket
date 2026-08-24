package com.mbeebe.docket.messaging;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.moderation.ReportableContent;
import com.mbeebe.docket.moderation.TargetKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The messaging module's answer to {@link ReportableContent} (§10.2, §10.3): the
 * MESSAGE kind, and only that one.
 *
 * <p>{@link #visibleToReporter} is participation in the Thread and nothing else —
 * §10.2's hard line, the same question {@link MessageImageAudience} asks. A
 * Message inside a Thread you are not part of is not reportable, because private
 * is private by construction; a stranger who somehow guesses an id gets the same
 * empty answer as for a Message that never existed. Deliberately insensitive to
 * Blocks and Disconnects, like the image port: §7.3 and §11.1 keep the history
 * readable to both sides after an ending, and being blocked afterwards must not
 * take away your ability to report what was said to you.
 *
 * <p>{@link #forModeration} sets participation aside, which is exactly the case
 * this seam exists for: the queue must be able to read a private Message that
 * was reported, and the {@code summary} is therefore the body in full.
 */
@Component
class MessageReportable implements ReportableContent {

    private final MessageRepository messages;
    private final MessageThreadRepository threads;

    MessageReportable(MessageRepository messages, MessageThreadRepository threads) {
        this.messages = messages;
        this.threads = threads;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> visibleToReporter(TargetKind kind, long id,
                                                    Optional<Member> viewer) {
        if (kind != TargetKind.MESSAGE || viewer.isEmpty()) {
            return Optional.empty();
        }
        return messages.findById(id)
                .filter(message -> !message.removed())
                .filter(message -> threads.findById(message.threadId())
                        .map(thread -> thread.includes(viewer.get().id()))
                        .orElse(false))
                .map(MessageReportable::item);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> forModeration(TargetKind kind, long id) {
        return kind == TargetKind.MESSAGE
                ? messages.findById(id).map(MessageReportable::item)
                : Optional.empty();
    }

    @Override
    @Transactional
    public boolean remove(TargetKind kind, long id, Instant now) {
        if (kind != TargetKind.MESSAGE) {
            return false;
        }
        messages.findById(id).ifPresent(message -> message.remove(now));
        return true;
    }

    @Override
    @Transactional
    public boolean restore(TargetKind kind, long id) {
        if (kind != TargetKind.MESSAGE) {
            return false;
        }
        messages.findById(id).ifPresent(Message::restore);
        return true;
    }

    /**
     * A Message has no page of its own: a Thread is reached by the person at the
     * other end of it, so the href is the correspondence with the Message's
     * author — which is the URL the reporter was already reading it at.
     */
    private static ReportedItem item(Message message) {
        return new ReportedItem(TargetKind.MESSAGE, message.id(),
                Optional.of(message.authorId()), message.body(),
                "/messages/" + message.authorId(), message.removed());
    }
}
