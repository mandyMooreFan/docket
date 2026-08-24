package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.profile.Capability;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reports and the ladder (§10.1–10.3): the reactive queue one person works, and the
 * four rungs that answer it.
 *
 * <p>Two properties are worth stating because they are enforced here rather than
 * remembered. First, a Report can only be raised against something the reporter can
 * already see — §10.2's line about Threads you are not part of is not a missing link in
 * the UI, it is a refusal at the service. Second, every rung records its reason, and
 * every rung that touches a Member sends them the statement of reasons in the same
 * transaction, because Art. 17 owes it "at the latest from the date the restriction is
 * imposed" and a later job would be a window in which it was owed and unsent.
 */
@Service
class ModerationService {

    private final ReportRepository reports;
    private final ModerationActionRepository actions;
    private final AppealRepository appeals;
    private final ReportableContents content;
    private final Terminations terminations;
    private final ModerationMails mails;
    private final Members members;
    private final Clock clock;

    ModerationService(ReportRepository reports, ModerationActionRepository actions,
                      AppealRepository appeals, ReportableContents content,
                      Terminations terminations, ModerationMails mails,
                      Members members, Clock clock) {
        this.reports = reports;
        this.actions = actions;
        this.appeals = appeals;
        this.content = content;
        this.terminations = terminations;
        this.mails = mails;
        this.members = members;
        this.clock = clock;
    }

    /** Raised when the product refuses, carrying the sentence the Member reads. */
    static class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    // ---- Reporting (§10.2) ------------------------------------------------------

    /**
     * Record a Report and acknowledge it. Returns empty when the reporter may not see
     * the item, which the route turns into a 404 — a report form that told you
     * something exists would be an enumeration surface (§8.5).
     */
    @Transactional
    Optional<Long> report(Member reporter, TargetKind kind, long targetId,
                          ReportCategory category, String account) {
        if (content.visibleToReporter(kind, targetId, Optional.of(reporter)).isEmpty()) {
            return Optional.empty();
        }
        String written = account == null ? "" : account.strip();
        if (written.isBlank()) {
            throw new Refused("Tell us what the problem is — a sentence is enough.");
        }
        Report report = reports.save(
                new Report(reporter.id(), kind, targetId, category, written, clock.instant()));
        mails.acknowledgeReport(reporter.email(), report);
        return Optional.of(report.id());
    }

    // ---- The queue (§10.1) ------------------------------------------------------

    @Transactional(readOnly = true)
    List<QueueEntry> queue() {
        return reports.findByDecidedAtIsNullOrderByCreatedAtAscIdAsc().stream()
                .map(this::entryFor)
                .toList();
    }

    @Transactional(readOnly = true)
    Optional<QueueEntry> queueEntry(long reportId) {
        return reports.findById(reportId).map(this::entryFor);
    }

    private QueueEntry entryFor(Report report) {
        return new QueueEntry(
                report.id(),
                report.targetKind(),
                report.category(),
                report.account(),
                report.createdAt(),
                content.forModeration(report.targetKind(), report.targetId()));
    }

    /**
     * Dismiss a Report: nothing was wrong, and the decision is still recorded with its
     * reason. Dismissals are counted in the transparency log exactly like upholdings —
     * a log that only counted actions taken would flatter the moderator.
     */
    @Transactional
    void dismiss(long reportId, String reason, Member actor) {
        Report report = openReport(reportId);
        report.decide(Report.Decision.DISMISSED, reason, actor.id(), clock.instant());
    }

    // ---- The ladder (§10.3) -----------------------------------------------------

    /** Rung 1: remove the item. Total and disclosed — there is no quieter option. */
    @Transactional
    void removeItem(long reportId, String reason, Member actor) {
        Report report = openReport(reportId);
        Instant now = clock.instant();
        if (!content.remove(report.targetKind(), report.targetId(), now)) {
            throw new Refused("Nothing removable is there any more.");
        }
        actions.save(ModerationAction.removal(report.id(), report.targetKind(),
                report.targetId(), reason, actor.id(), now));
        report.decide(Report.Decision.UPHELD, reason, actor.id(), now);
        tellTheAuthor(report, "An item you posted has been removed from Docket.", reason, null);
    }

    /**
     * Rung 2: withdraw the specific Capability that was abused, for a stated period or
     * indefinitely. Proportionality is the whole point — §10.3's example is that
     * someone abusing job postings does not thereby lose their correspondence.
     */
    @Transactional
    void withdraw(long reportId, Capability capability, Optional<Duration> period,
                  String reason, Member actor) {
        Report report = openReport(reportId);
        long memberId = answerableFor(report);
        Instant now = clock.instant();
        Instant until = period.map(now::plus).orElse(null);
        ModerationAction action = actions.save(ModerationAction.withdrawal(
                report.id(), memberId, capability, until, reason, actor.id(), now));
        report.decide(Report.Decision.UPHELD, reason, actor.id(), now);
        tellTheAuthor(report, withdrawalSentence(capability, Optional.ofNullable(until)),
                reason, action.id());
    }

    /** Rung 3: read-only. They may still sign in — that is what makes it not rung 4. */
    @Transactional
    void suspend(long reportId, Optional<Duration> period, String reason, Member actor) {
        Report report = openReport(reportId);
        long memberId = answerableFor(report);
        Instant now = clock.instant();
        Instant until = period.map(now::plus).orElse(null);
        ModerationAction action = actions.save(ModerationAction.suspension(
                report.id(), memberId, until, reason, actor.id(), now));
        report.decide(Report.Decision.UPHELD, reason, actor.id(), now);
        tellTheAuthor(report, """
                Your account is read-only %s. You can still sign in and read \
                everything you could before; you cannot post, reply, message or \
                connect.""".formatted(untilPhrase(Optional.ofNullable(until))), reason, action.id());
    }

    /** Rung 4: the end of a Member. */
    @Transactional
    void terminate(long reportId, String reason, Member actor) {
        Report report = openReport(reportId);
        long memberId = answerableFor(report);
        Instant now = clock.instant();
        ModerationAction action = actions.save(
                ModerationAction.termination(report.id(), memberId, reason, actor.id(), now));
        report.decide(Report.Decision.UPHELD, reason, actor.id(), now);
        // The statement of reasons goes before the sessions end: Art. 17 owes it from
        // the moment the restriction takes effect, and afterwards is not that moment.
        tellTheAuthor(report, "Your Docket account has been terminated.", reason, action.id());
        terminations.terminateByModeration(memberId, reason);
    }

    // ---- Appeals (§10.3) --------------------------------------------------------

    /**
     * One Appeal, and the uniqueness is the database's. A Member may appeal a rung that
     * touched them; a Report's dismissal is not appealable by the reporter, because
     * nothing was restricted and Art. 17's redress duty is owed to the restricted.
     */
    @Transactional
    long appeal(Member member, long actionId, String account) {
        ModerationAction action = actions.findById(actionId)
                .orElseThrow(() -> new Refused("There is no such decision."));
        if (action.memberId() == null || action.memberId() != member.id()) {
            throw new Refused("There is no such decision.");
        }
        if (appeals.existsByActionId(actionId)) {
            throw new Refused("You have already appealed this one. There is one appeal, "
                    + "and this was it.");
        }
        String written = account == null ? "" : account.strip();
        if (written.isBlank()) {
            throw new Refused("Tell us what we should reconsider.");
        }
        return appeals.save(new Appeal(actionId, member.id(), written, clock.instant())).id();
    }

    @Transactional(readOnly = true)
    List<AppealView> openAppeals() {
        return appeals.findByDecidedAtIsNullOrderByMadeAtAscIdAsc().stream()
                .map(appeal -> new AppealView(appeal.id(), appeal.account()))
                .toList();
    }

    /**
     * Decide an Appeal. Upholding reverses the rung without erasing that it happened —
     * the action row keeps its {@code actedAt} and gains a {@code reversedAt}, so the
     * transparency log stays truthful about what was done at the time.
     */
    @Transactional
    void decideAppeal(long appealId, Appeal.Outcome outcome, String reason) {
        Appeal appeal = appeals.findById(appealId)
                .orElseThrow(() -> new Refused("There is no such appeal."));
        Instant now = clock.instant();
        appeal.decide(outcome, reason, now);
        if (outcome == Appeal.Outcome.UPHELD) {
            actions.findById(appeal.actionId()).ifPresent(action -> action.reverse(now));
        }
        members.find(appeal.memberId()).ifPresent(member -> mails.statementOfReasons(
                member.email(),
                outcome == Appeal.Outcome.UPHELD
                        ? "Your appeal succeeded, and the decision has been reversed."
                        : "Your appeal did not succeed, and the decision stands.",
                reason, false, Optional.empty()));
    }

    // ---- Internals --------------------------------------------------------------

    private Report openReport(long reportId) {
        Report report = reports.findById(reportId)
                .orElseThrow(() -> new Refused("There is no such report."));
        if (!report.open()) {
            throw new Refused("That report has already been decided.");
        }
        return report;
    }

    /**
     * The Member a rung can be applied to. A Company page has no author (§6.1: written
     * by many hands, named by none), so the member-facing rungs simply do not reach it
     * — the answer is to remove the page or merge it, not to punish a person for it.
     */
    private long answerableFor(Report report) {
        return content.forModeration(report.targetKind(), report.targetId())
                .flatMap(ReportableContent.ReportedItem::authorId)
                .orElseThrow(() -> new Refused(
                        "Nobody is answerable for that on their own — remove it instead."));
    }

    private void tellTheAuthor(Report report, String whatWasDone, String reason, Long actionId) {
        content.forModeration(report.targetKind(), report.targetId())
                .flatMap(ReportableContent.ReportedItem::authorId)
                .flatMap(members::find)
                .ifPresent(author -> mails.statementOfReasons(author.email(), whatWasDone,
                        reason, true, Optional.ofNullable(actionId)));
    }

    private String withdrawalSentence(Capability capability, Optional<Instant> until) {
        String what = switch (capability) {
            case CONNECT -> "send connection requests";
            case INVITE -> "send invites";
            case MESSAGE -> "send messages";
            case POST -> "write posts";
            case POST_JOB -> "post jobs";
            case REPLY -> "write replies";
        };
        return "You can no longer %s %s. Everything else about your account is "
                .formatted(what, untilPhrase(until))
                + "unchanged — this took away the one thing that was misused, and nothing else.";
    }

    private String untilPhrase(Optional<Instant> until) {
        return until.isEmpty() ? "indefinitely" : "until " + when(until.get());
    }

    private String when(Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone()).format(instant);
    }

}
