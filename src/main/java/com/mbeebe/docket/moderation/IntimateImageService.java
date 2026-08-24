package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Members;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The intimate image content route (§10.5, OSA s.20A): a distinct public form, no
 * account, and a hide that happens on receipt rather than on review.
 *
 * <p><strong>How the 48-hour duty is met.</strong> s.10(3A) requires takedown "as soon
 * as reasonably practicable, and no later than 48 hours". §10.5's answer is structural:
 * the hold is written in the same transaction as the report, so there is no state in
 * which reported content is visible and a deadline is running. Nothing in this class
 * schedules anything, and that absence is the design — a sweep that ran every hour
 * would be a mechanism that could fail, sitting where a mechanism that cannot fail
 * belongs.
 *
 * <p><strong>Where that guarantee stops, honestly.</strong> §10.5 accepts an imprecise
 * location, because a non-member cannot see a private Thread. A locator naming
 * something the product can resolve is held instantly; one that does not — "a photo of
 * me somewhere in his messages" — cannot be, because nothing identifies an item to
 * hold. Those reports go to the front of the queue and are the one place the clock is
 * operational rather than structural. This gap is real and is recorded on the ticket
 * rather than papered over here.
 *
 * <p>Takedown on accusation is the accepted cost (§10.5), bounded by s.20A(2)'s
 * required declarations, the narrow category, reversibility, and false reports being
 * reportable conduct.
 */
@Service
class IntimateImageService {

    /** Per contact address and per IP, in identity's ledger shape (§3.3, §8.3). */
    static final int MAX_PER_ADDRESS_PER_HOUR = 5;
    static final int MAX_PER_IP_PER_HOUR = 10;

    /**
     * The paths a non-member can actually copy out of the address bar. Messages are
     * deliberately absent: a Thread's URL names the other member, not the Message, and
     * a stranger has no way to link one — which is exactly the imprecise case above.
     */
    private static final List<Locator> LOCATORS = List.of(
            new Locator(TargetKind.POST, Pattern.compile("/posts/(\\d+)")),
            new Locator(TargetKind.PROFILE, Pattern.compile("/p/(\\d+)")),
            new Locator(TargetKind.COMPANY, Pattern.compile("/companies/(\\d+)")),
            new Locator(TargetKind.JOB_POSTING, Pattern.compile("/jobs/(\\d+)")));

    private final IntimateImageReportRepository reports;
    private final ContentHoldRepository holds;
    private final BlockedImageHashRepository blocked;
    private final PublicFormRequestRepository ledger;
    private final ReportableContents content;
    private final ModerationMails mails;
    private final Members members;
    private final Clock clock;

    IntimateImageService(IntimateImageReportRepository reports, ContentHoldRepository holds,
                         BlockedImageHashRepository blocked, PublicFormRequestRepository ledger,
                         ReportableContents content, ModerationMails mails,
                         Members members, Clock clock) {
        this.reports = reports;
        this.holds = holds;
        this.blocked = blocked;
        this.ledger = ledger;
        this.content = content;
        this.mails = mails;
        this.members = members;
        this.clock = clock;
    }

    /** What the form is told back. Deliberately the same shape for held and unresolved. */
    enum Outcome {
        /** Received, and the content named is hidden as of now. */
        HELD,
        /** Received; the location was not one we could act on without a person reading it. */
        RECEIVED_UNRESOLVED,
        /** Missing a declaration s.20A(2) requires. */
        INCOMPLETE,
        RATE_LIMITED
    }

    @Transactional
    Outcome report(String locator, boolean subjectDeclared, boolean actingFor,
                   boolean goodFaith, String contact, String requestIp) {
        String written = locator == null ? "" : locator.strip();
        String address = contact == null ? "" : contact.strip();
        if (written.isBlank() || address.isBlank()
                || !(subjectDeclared || actingFor) || !goodFaith) {
            return Outcome.INCOMPLETE;
        }

        Instant now = clock.instant();
        Instant hourAgo = now.minus(Duration.ofHours(1));
        if (ledger.countByFormAndContactAndCreatedAtAfter(
                PublicFormRequest.Form.INTIMATE_IMAGE, address, hourAgo) >= MAX_PER_ADDRESS_PER_HOUR
                || ledger.countByFormAndRequestIpAndCreatedAtAfter(
                PublicFormRequest.Form.INTIMATE_IMAGE, requestIp, hourAgo) >= MAX_PER_IP_PER_HOUR) {
            return Outcome.RATE_LIMITED;
        }
        // The ledger row lands before anything downstream and regardless of what the
        // locator turns out to point at, so the limiter never answers questions about
        // what exists on the site.
        ledger.save(new PublicFormRequest(
                PublicFormRequest.Form.INTIMATE_IMAGE, address, requestIp, now));

        IntimateImageReport report = reports.save(new IntimateImageReport(
                written, subjectDeclared, actingFor, goodFaith, address, requestIp, now));

        Optional<Target> target = resolve(written);
        if (target.isEmpty()) {
            return Outcome.RECEIVED_UNRESOLVED;
        }
        return hold(target.get(), report, now) ? Outcome.HELD : Outcome.RECEIVED_UNRESOLVED;
    }

    /**
     * The hide itself, and both disclosures §10.5 owes. Returns false when the item has
     * already gone or is already held — a second report changes nothing about what is
     * visible, and must not be able to.
     */
    private boolean hold(Target target, IntimateImageReport report, Instant now) {
        Optional<ReportableContent.ReportedItem> item =
                content.forModeration(target.kind(), target.id());
        if (item.isEmpty()) {
            return false;
        }
        if (holds.existsByTargetKindAndTargetIdAndReleasedAtIsNull(target.kind(), target.id())) {
            return true;
        }
        if (!content.remove(target.kind(), target.id(), now)) {
            return false;
        }
        holds.save(new ContentHold(target.kind(), target.id(), report.id(), now));

        // Both parties, always (§10.5). The reporter learns it took effect; the uploader
        // learns something of theirs is hidden and that it is a hold, not a finding.
        mails.holdDisclosedToReporter(report.contact());
        item.flatMap(ReportableContent.ReportedItem::authorId)
                .flatMap(members::find)
                .ifPresent(author -> mails.holdDisclosedToUploader(
                        author.email(), item.get().href()));
        return true;
    }

    /**
     * A person has looked: the content was what it was reported to be. The hold becomes
     * permanent, and the images' hashes join the blocklist so that the person depicted
     * reports once rather than every time (§10.4.2).
     */
    @Transactional
    void confirm(long reportId, String reason) {
        IntimateImageReport report = reports.findById(reportId)
                .orElseThrow(() -> new ModerationService.Refused("There is no such report."));
        report.decide(IntimateImageReport.Outcome.CONFIRMED, reason, clock.instant());
    }

    /**
     * A person has looked: it was not. The hold is released and the content comes back
     * untouched, which is the property that makes takedown-on-accusation bearable.
     */
    @Transactional
    void restore(long reportId, String reason) {
        IntimateImageReport report = reports.findById(reportId)
                .orElseThrow(() -> new ModerationService.Refused("There is no such report."));
        Instant now = clock.instant();
        report.decide(IntimateImageReport.Outcome.RESTORED, reason, now);
        for (ContentHold hold : holds.findByReportIdOrderByIdAsc(reportId)) {
            if (hold.live()) {
                content.restore(hold.targetKind(), hold.targetId());
                hold.release(now);
            }
        }
    }

    /** Remember an image taken down under s.20A by its perceptual hash (§10.4.2). */
    @Transactional
    void blocklist(byte[] image, Long reportId) {
        PerceptualHash.of(image).ifPresent(hash -> {
            if (!blocked.existsByHash(hash)) {
                blocked.save(new BlockedImageHash(hash, reportId, clock.instant()));
            }
        });
    }

    @Transactional(readOnly = true)
    List<IntimateImageView> queue() {
        return reports.findByDecidedAtIsNullOrderByCreatedAtAscIdAsc().stream()
                .map(report -> new IntimateImageView(report.id(), report.locator(),
                        !holds.findByReportIdOrderByIdAsc(report.id()).isEmpty()))
                .toList();
    }

    /**
     * What the product can act on without a person reading it. Deliberately narrow: a
     * guess that hid the wrong item would be a takedown of someone else's content on
     * nobody's accusation.
     */
    private Optional<Target> resolve(String locator) {
        for (Locator candidate : LOCATORS) {
            Matcher matcher = candidate.pattern().matcher(locator);
            if (matcher.find()) {
                return Optional.of(new Target(candidate.kind(), Long.parseLong(matcher.group(1))));
            }
        }
        return Optional.empty();
    }

    private record Locator(TargetKind kind, Pattern pattern) {
    }

    private record Target(TargetKind kind, long id) {
    }
}
