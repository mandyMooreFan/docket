package com.mbeebe.docket.messaging;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.images.Images;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.ConnectionLookup;
import com.mbeebe.docket.profile.ProfileService;
import com.mbeebe.docket.text.Prose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Messaging (SPEC.md §7) on ADR-0001: exactly one permanent Thread per pair of
 * Members, and one gate in front of it — a Connection, or an open Application.
 * Every authorisation question is answered from stored facts at the point of
 * asking (ADR-0002); no Thread carries a writable flag, so Disconnect, Block,
 * reconnection and an Application's Outcome all take effect with no write to
 * any Thread anywhere.
 *
 * <p>Nothing in this class touches the Mailer, and nothing ever will: §7.4 is
 * "an in-app Unread count on the inbox, and nothing else — no email at all,
 * not even a first message from a new Connection".
 */
@Service
class MessagingService {

    static final int MAX_BODY = 8_000;
    static final int MAX_IMAGES = 4;

    /**
     * The one sentence a closed Thread shows, whatever closed it. A Disconnect
     * and a Block are worded identically on purpose (§7.3 + §4.2): the blocked
     * party must not be able to tell which happened to them.
     */
    static final String CLOSED_NOTE =
            "This thread is closed to new messages. The history stays here for both of you.";

    static final String NO_CAPABILITY_NOTE = "Messaging opens when your profile is complete.";

    /** A refusal with an honest, member-facing reason; rolls the whole write back. */
    static class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    /** Why this pair may or may not write right now — derived, never stored. */
    enum Writability { OPEN, CLOSED, NO_CAPABILITY }

    private final MessageThreadRepository threads;
    private final MessageRepository messages;
    private final MessageImageRepository messageImages;
    private final ThreadReadRepository marks;
    private final ConnectionLookup graph;
    private final ApplicationChannel applications;
    private final CapabilityService capabilities;
    private final ProfileService profiles;
    private final Members members;
    private final Images images;
    private final Clock clock;

    MessagingService(MessageThreadRepository threads, MessageRepository messages,
                     MessageImageRepository messageImages, ThreadReadRepository marks,
                     ConnectionLookup graph, ApplicationChannel applications,
                     CapabilityService capabilities, ProfileService profiles, Members members,
                     Images images, Clock clock) {
        this.threads = threads;
        this.messages = messages;
        this.messageImages = messageImages;
        this.marks = marks;
        this.graph = graph;
        this.applications = applications;
        this.capabilities = capabilities;
        this.profiles = profiles;
        this.members = members;
        this.images = images;
        this.clock = clock;
    }

    /**
     * §7.1, the whole gate: a Connection or an open Application, and a Block
     * beats both. Nothing else opens a channel — no InMail, no paid reach, no
     * requests queue — so a poster can never reach someone who did not apply.
     */
    boolean channelOpen(long viewerId, long otherId) {
        if (viewerId == otherId || members.find(otherId).isEmpty()) {
            return false;
        }
        if (graph.blocked(viewerId, otherId)) {
            return false;
        }
        return graph.connected(viewerId, otherId) || applications.openBetween(viewerId, otherId);
    }

    /**
     * The channel, plus §3.2's MESSAGE capability on the writer. Order matters:
     * a closed channel is reported as closed whatever the writer's profile
     * says, so an incomplete profile can never be used to distinguish a Block
     * from a Disconnect from a stranger.
     */
    @Transactional(readOnly = true)
    Writability writability(long viewerId, long otherId) {
        if (!channelOpen(viewerId, otherId)) {
            return Writability.CLOSED;
        }
        return capabilities.may(viewerId, Capability.MESSAGE) == CapabilityAnswer.YES
                ? Writability.OPEN
                : Writability.NO_CAPABILITY;
    }

    /**
     * The Thread with one other Member, as this Member sees it. Empty — a plain
     * 404, no placeholder — when there is neither a correspondence to read nor
     * a channel to start one on. Reading advances this Member's own read mark
     * and nothing else: the other side's rendering is untouched (§7.2).
     */
    @Transactional
    Optional<ThreadPage> threadFor(Member viewer, long otherId) {
        if (viewer.id() == otherId || members.find(otherId).isEmpty()) {
            return Optional.empty();
        }
        Optional<MessageThread> thread = findThread(viewer.id(), otherId);
        boolean open = channelOpen(viewer.id(), otherId);
        if (thread.isEmpty() && !open) {
            return Optional.empty();
        }
        List<MessageView> views = thread.map(found -> render(found, viewer.id()))
                .orElseGet(List::of);
        Writability writability = writability(viewer.id(), otherId);
        return Optional.of(new ThreadPage(otherId, profiles.cardFor(otherId), views,
                writability == Writability.OPEN, noteFor(writability)));
    }

    /** The polled fragment (§7.2, §14.2): the same message list, nothing more. */
    @Transactional
    Optional<List<MessageView>> messagesFor(Member viewer, long otherId) {
        return threadFor(viewer, otherId).map(ThreadPage::messages);
    }

    private static String noteFor(Writability writability) {
        return switch (writability) {
            case OPEN -> "";
            case CLOSED -> CLOSED_NOTE;
            case NO_CAPABILITY -> NO_CAPABILITY_NOTE;
        };
    }

    /**
     * §7.2: text, links and still images. The Thread is created lazily here —
     * the first authorised write — and found, never re-created, every time
     * after, which is ADR-0001's "one Thread per pair, ever" in one line.
     * Images go through the one §10.4 store; any refusal refuses the whole
     * Message, so nothing partial ever lands.
     */
    @Transactional
    void send(Member author, long otherId, String rawBody, List<MultipartFile> files) {
        String body = rawBody == null ? "" : rawBody.strip();
        if (body.isEmpty()) {
            throw new Refused("A message needs words.");
        }
        if (body.length() > MAX_BODY) {
            throw new Refused("A message can hold at most 8,000 characters.");
        }
        MessageThread thread = findThread(author.id(), otherId)
                .orElseGet(() -> threads.save(
                        MessageThread.between(author.id(), otherId, clock.instant())));
        Message message = messages.save(
                new Message(thread.id(), author.id(), body, clock.instant()));
        int position = 0;
        for (MultipartFile file : files == null ? List.<MultipartFile>of() : files) {
            if (file.isEmpty()) {
                continue;
            }
            if (position == MAX_IMAGES) {
                throw new Refused("A message can carry at most four images.");
            }
            storeImage(message.id(), file, position++);
        }
    }

    private void storeImage(long messageId, MultipartFile file, int position) {
        Images.Stored stored;
        try {
            stored = images.store(file.getBytes(), file.getContentType());
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
        switch (stored.outcome()) {
            case STORED -> messageImages.save(
                    new MessageImage(messageId, stored.imageId(), position));
            case TOO_LARGE -> throw new Refused("Images are capped at 512KB.");
            case WRONG_TYPE -> throw new Refused("Images are PNG or JPEG.");
            case REFUSED -> throw new Refused("An image was refused by the upload checks.");
        }
    }

    /**
     * The inbox (§7.2): a list of people, ordered by the latest thing said.
     * Closed Threads stay listed — the correspondence you had is yours (§7.3).
     */
    @Transactional(readOnly = true)
    InboxPage inboxFor(Member viewer) {
        List<InboxPage.Row> rows = threads.findByMemberAOrMemberB(viewer.id(), viewer.id())
                .stream()
                .flatMap(thread -> messages
                        .findFirstByThreadIdAndRemovedAtIsNullOrderByIdDesc(thread.id())
                        .map(latest -> new Latest(thread, latest)).stream())
                .sorted(Comparator.comparingLong((Latest latest) -> latest.message().id()).reversed())
                .map(latest -> new InboxPage.Row(latest.thread().other(viewer.id()),
                        profiles.cardFor(latest.thread().other(viewer.id())),
                        Prose.excerpt(latest.message().body()),
                        when(latest.message().createdAt())))
                .toList();
        return new InboxPage(rows);
    }

    private record Latest(MessageThread thread, Message message) {
    }

    /**
     * §7.4's Unread count — the product's only badge, and only ever this one
     * number on the Messages nav item. Zero returns empty so that "nothing at
     * zero" is structural rather than a template's good manners.
     */
    @Transactional(readOnly = true)
    Optional<Integer> unreadFor(long memberId) {
        long unread = messages.unreadFor(memberId);
        return unread > 0 ? Optional.of((int) unread) : Optional.empty();
    }

    private Optional<MessageThread> findThread(long one, long other) {
        return threads.findByMemberAAndMemberB(Math.min(one, other), Math.max(one, other));
    }

    /**
     * Renders the correspondence and advances the reader's own mark to the last
     * Message the rendering actually showed them. Deliberately one-sided: no
     * row the other person can see changes, now or ever (§7.2). A removed
     * Message (§10.3 rung 1) is absent for both correspondents — the predicate
     * is the query's, so the mark can only ever reach a Message that rendered.
     */
    private List<MessageView> render(MessageThread thread, long viewerId) {
        List<Message> found = messages.findByThreadIdAndRemovedAtIsNullOrderByIdAsc(thread.id());
        if (!found.isEmpty()) {
            markRead(thread.id(), viewerId, found.get(found.size() - 1).id());
        }
        return found.stream()
                .map(message -> new MessageView(message.id(),
                        profiles.cardFor(message.authorId()),
                        message.authorId() == viewerId,
                        when(message.createdAt()),
                        Prose.toHtml(message.body()),
                        messageImages.findByMessageIdOrderByPosition(message.id()).stream()
                                .map(MessageImage::imageId).toList()))
                .toList();
    }

    private void markRead(long threadId, long memberId, long messageId) {
        marks.findByThreadIdAndMemberId(threadId, memberId)
                .ifPresentOrElse(mark -> mark.advanceTo(messageId),
                        () -> marks.save(new ThreadRead(threadId, memberId, messageId)));
    }

    private String when(Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.UK)
                .withZone(clock.getZone()).format(instant);
    }
}
