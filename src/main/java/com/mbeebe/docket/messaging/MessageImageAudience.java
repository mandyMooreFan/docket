package com.mbeebe.docket.messaging;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.ImageAudience;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The messaging module's answer for /images/{id} (§7.2, §10.2): an image on a
 * Message is for the two people in that Thread, and for nobody else ever.
 *
 * <p>Private correspondence is the strictest case this port has to carry, and
 * the rule is correspondingly short — participation, asked of the Thread the
 * Message sits in. Never {@link Verdict#OPEN_WEB}: nothing said inside a Thread
 * is on the open web, so nothing from one may ride a shared cache. Deliberately
 * not sensitive to Blocks or Disconnects either: §7.3 and §11.1 keep the
 * history readable to both sides after an ending, and an image that vanished
 * from a kept correspondence would be one person destroying the other's record.
 */
@Component
class MessageImageAudience implements ImageAudience {

    private final MessageImageRepository messageImages;
    private final MessageRepository messages;
    private final MessageThreadRepository threads;

    MessageImageAudience(MessageImageRepository messageImages, MessageRepository messages,
                         MessageThreadRepository threads) {
        this.messageImages = messageImages;
        this.messages = messages;
        this.threads = threads;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Verdict> verdictFor(long imageId, Optional<Member> viewer) {
        return messageImages.findFirstByImageId(imageId)
                .flatMap(attachment -> messages.findById(attachment.messageId()))
                .flatMap(message -> threads.findById(message.threadId()))
                .map(thread -> viewer
                        .filter(member -> thread.includes(member.id()))
                        .isPresent() ? Verdict.THIS_VIEWER : Verdict.HIDDEN);
    }
}
