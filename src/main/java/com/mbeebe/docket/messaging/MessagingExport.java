package com.mbeebe.docket.messaging;

import com.mbeebe.docket.leaving.ExportContributor;
import com.mbeebe.docket.leaving.ExportDates;
import com.mbeebe.docket.leaving.ExportField;
import com.mbeebe.docket.leaving.ExportMedia;
import com.mbeebe.docket.leaving.ExportRecord;
import com.mbeebe.docket.leaving.ExportSection;
import com.mbeebe.docket.profile.PersonCard;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Threads, exported whole — <strong>both halves</strong> (SPEC.md §11.1).
 *
 * <p>The instinct that a two-person Thread cannot be exported because it contains
 * someone else's words is wrong, and the guidance says so directly. WP242 rev.01
 * p.9, reproduced verbatim by the EDPB in Guidelines 01/2022: interpersonal
 * messaging records go to the subscriber <em>because they are (also) concerning
 * the data subject</em>. Art. 20(4)'s "rights and freedoms of others" proviso
 * targets what the receiving controller does with the data, not whether the export
 * happens; a blanket refusal is not available (Recital 63, EDPB ¶172: "the general
 * concern that rights and freedoms of others might be affected … is not enough").
 * Half a conversation would also not be a record of anything.
 *
 * <p>What the guidance <em>does</em> attach is a purpose condition, and that is
 * {@code ExportSection.CORRESPONDENCE_NOTE} — supplied by the factory rather than
 * passed in, so a section carrying somebody else's words cannot be built without
 * it. It is spec copy, not a UI detail (§11.1).
 *
 * <p>This is the archive's only {@link ExportSection.Kind#CORRESPONDENCE} section,
 * and that is what puts it in its own file behind that note.
 *
 * <p>Read marks are absent, and permanently: §7.2 has no read receipts, ever. An
 * export of "when you read their message" would be the product's one refused
 * feature, delivered by the back door.
 */
@Component
@Order(70)
class MessagingExport implements ExportContributor {

    private final MessageThreadRepository threads;
    private final MessageRepository messages;
    private final MessageImageRepository messageImages;
    private final ProfileService profiles;
    private final Clock clock;

    MessagingExport(MessageThreadRepository threads, MessageRepository messages,
                    MessageImageRepository messageImages, ProfileService profiles,
                    Clock clock) {
        this.threads = threads;
        this.messages = messages;
        this.messageImages = messageImages;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        List<ExportRecord> records =
                threads.findByMemberAOrMemberB(memberId, memberId).stream()
                        .map(thread -> thread(thread, memberId))
                        .toList();
        return List.of(
                ExportSection.correspondence("threads", "Your conversations", records));
    }

    private ExportRecord thread(MessageThread thread, long memberId) {
        PersonCard other = profiles.cardFor(thread.other(memberId));
        return ExportRecord.nesting("With " + other.displayName(),
                List.of(ExportField.of("with_member_id", "Member number", other.memberId()),
                        ExportField.of("with", "With", other.displayName()),
                        ExportField.of("started", "Started",
                                ExportDates.on(thread.createdAt(), clock))),
                messages.findByThreadIdOrderByIdAsc(thread.id()).stream()
                        .map(message -> message(message, memberId, other))
                        .toList());
    }

    private ExportRecord message(Message message, long memberId, PersonCard other) {
        boolean mine = message.authorId() == memberId;
        return new ExportRecord(mine ? "You" : other.displayName(),
                List.of(ExportField.of("from", "From", mine ? "You" : other.displayName()),
                        ExportField.of("sent", "Sent",
                                ExportDates.at(message.createdAt(), clock)),
                        ExportField.of("body", "Message", message.body())),
                List.of(),
                messageImages.findByMessageIdOrderByPosition(message.id()).stream()
                        .map(image -> ExportMedia.of(image.imageId(), "message"))
                        .toList());
    }
}
