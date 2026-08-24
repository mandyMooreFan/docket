package com.mbeebe.docket.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Termination (CONTEXT.md): the end of a Member. One row per Member, ever.
 *
 * <p>The cause distinguishes §10.3's fourth rung from §11.2's own front door. Both
 * arrive through {@link Terminations}, so the two efforts do not build the primitive
 * twice: moderation writes {@code MODERATION} alongside its action row, and the
 * member-facing flow writes {@code MEMBER}.
 *
 * <p>Terminating deletes nothing. Member references across the product deliberately do
 * not cascade, because §11.2 keeps a former Member's side of a Thread and the
 * Recommendations they wrote, and neither person may destroy the other's record
 * (§7.3, §11.1).
 */
@Entity
@Table(name = "member_termination")
class MemberTermination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private Cause cause;

    private String reason = "";

    private Instant terminatedAt;

    enum Cause {
        /** §10.3's fourth rung. */
        MODERATION,
        /** §11.2: the Member's own decision to leave. */
        MEMBER
    }

    protected MemberTermination() {
    }

    MemberTermination(long memberId, Cause cause, String reason, Instant now) {
        this.memberId = memberId;
        this.cause = cause;
        this.reason = reason;
        this.terminatedAt = now;
    }

    long memberId() {
        return memberId;
    }

    Cause cause() {
        return cause;
    }

    Instant terminatedAt() {
        return terminatedAt;
    }
}
