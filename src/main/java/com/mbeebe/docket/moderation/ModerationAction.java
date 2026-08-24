package com.mbeebe.docket.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.mbeebe.docket.profile.Capability;

import java.time.Instant;

/**
 * One rung of §10.3's ladder, as a dated fact: a Removal, a Withdrawal, a Suspension or
 * a Termination. One entity rather than four, because the Appeal, the audit trail and
 * the transparency log all want to ask "what was done, to whom, and is it still in
 * force" without first knowing which rung it was.
 *
 * <p>Nothing here can express a reach reduction. §10.3 refuses visibility limiting and
 * shadowbanning outright — "covert reach reduction is a lie told to a member about
 * their own account" — and the refusal is kept honest by there being no column for it:
 * the only content rung is REMOVAL, which is total and disclosed.
 *
 * <p>{@code reversedAt} is how an upheld Appeal undoes a rung without erasing that it
 * happened, which is what keeps the transparency log truthful about actions taken.
 */
@Entity
@Table(name = "moderation_action")
class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reportId;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private TargetKind targetKind;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private Capability capability;

    private Instant until;

    private String reason;

    private Long actorId;

    private Instant actedAt;

    private Instant reversedAt;

    /** The four rungs, in order of severity (§10.3). */
    enum Kind {
        REMOVAL,
        WITHDRAWAL,
        SUSPENSION,
        TERMINATION
    }

    protected ModerationAction() {
    }

    private ModerationAction(Kind kind, Long reportId, Long memberId, TargetKind targetKind,
                             Long targetId, Capability capability, Instant until,
                             String reason, long actorId, Instant now) {
        this.kind = kind;
        this.reportId = reportId;
        this.memberId = memberId;
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.capability = capability;
        this.until = until;
        this.reason = reason;
        this.actorId = actorId;
        this.actedAt = now;
    }

    /** Rung 1: remove the item. */
    static ModerationAction removal(Long reportId, TargetKind targetKind, long targetId,
                                    String reason, long actorId, Instant now) {
        return new ModerationAction(Kind.REMOVAL, reportId, null, targetKind, targetId,
                null, null, reason, actorId, now);
    }

    /** Rung 2: withdraw the specific Capability that was abused, for a period or indefinitely. */
    static ModerationAction withdrawal(Long reportId, long memberId, Capability capability,
                                       Instant until, String reason, long actorId, Instant now) {
        return new ModerationAction(Kind.WITHDRAWAL, reportId, memberId, null, null,
                capability, until, reason, actorId, now);
    }

    /** Rung 3: read-only. The Member may still sign in (CONTEXT.md). */
    static ModerationAction suspension(Long reportId, long memberId, Instant until,
                                       String reason, long actorId, Instant now) {
        return new ModerationAction(Kind.SUSPENSION, reportId, memberId, null, null,
                null, until, reason, actorId, now);
    }

    /** Rung 4: the end of a Member. Cannot expire, so it never carries an until. */
    static ModerationAction termination(Long reportId, long memberId, String reason,
                                        long actorId, Instant now) {
        return new ModerationAction(Kind.TERMINATION, reportId, memberId, null, null,
                null, null, reason, actorId, now);
    }

    Long id() {
        return id;
    }

    Long reportId() {
        return reportId;
    }

    Kind kind() {
        return kind;
    }

    Long memberId() {
        return memberId;
    }

    TargetKind targetKind() {
        return targetKind;
    }

    Long targetId() {
        return targetId;
    }

    Capability capability() {
        return capability;
    }

    Instant until() {
        return until;
    }

    String reason() {
        return reason;
    }

    Instant actedAt() {
        return actedAt;
    }

    Instant reversedAt() {
        return reversedAt;
    }

    /**
     * Whether this rung is standing right now: not reversed, and either indefinite or
     * still inside its stated period. Derived at the ask (ADR-0002) — an expired
     * Withdrawal needs no sweep to stop biting, which is why nothing schedules one.
     */
    boolean inForceAt(Instant now) {
        return reversedAt == null && (until == null || now.isBefore(until));
    }

    /** Undo the rung without erasing it — an upheld Appeal, or a change of mind. */
    void reverse(Instant now) {
        if (reversedAt == null) {
            reversedAt = now;
        }
    }
}
