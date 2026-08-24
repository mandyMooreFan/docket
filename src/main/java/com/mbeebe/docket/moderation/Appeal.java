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
 * An Appeal (§10.3, CONTEXT.md): a Member's request that a moderation decision be
 * reconsidered, by the person who made it. Never a dispute, a review or an escalation —
 * and the product says so in those words, because §10.3 requires it be "described as
 * what it is: the same person reconsidering with new information".
 *
 * <p>"One Appeal" is a unique index on the action, not a rule someone remembers to
 * apply. A second attempt fails in the database.
 */
@Entity
@Table(name = "appeal")
class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actionId;

    private Long memberId;

    private String account;

    private Instant madeAt;

    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    private String reason = "";

    /** Upheld reverses the rung; refused leaves it standing. Both are recorded. */
    enum Outcome {
        UPHELD,
        REFUSED
    }

    protected Appeal() {
    }

    Appeal(long actionId, long memberId, String account, Instant now) {
        this.actionId = actionId;
        this.memberId = memberId;
        this.account = account;
        this.madeAt = now;
    }

    Long id() {
        return id;
    }

    long actionId() {
        return actionId;
    }

    long memberId() {
        return memberId;
    }

    String account() {
        return account;
    }

    Instant madeAt() {
        return madeAt;
    }

    Outcome outcome() {
        return outcome;
    }

    String reason() {
        return reason;
    }

    boolean open() {
        return decidedAt == null;
    }

    void decide(Outcome outcome, String reason, Instant now) {
        if (decidedAt == null) {
            this.outcome = outcome;
            this.reason = reason;
            this.decidedAt = now;
        }
    }
}
