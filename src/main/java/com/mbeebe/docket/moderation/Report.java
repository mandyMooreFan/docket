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
 * A Report (§10.2, CONTEXT.md): a Member's account of something they believe breaks a
 * rule. Never a flag, a complaint or a ticket — it is an account, and the field is
 * named for what it is.
 *
 * <p>{@code acknowledgedAt} is set at construction and never null. DSA Art. 16's
 * confirmation of receipt is therefore not a job that might not run: a Report that
 * exists has been acknowledged, in the same transaction, or it does not exist.
 */
@Entity
@Table(name = "report")
class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reporterId;

    @Enumerated(EnumType.STRING)
    private TargetKind targetKind;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private ReportCategory category;

    private String account;

    private Instant createdAt;

    private Instant acknowledgedAt;

    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    private Decision decision;

    private String decisionReason = "";

    private Long decidedBy;

    /** What the owner concluded (§10.1) — recorded with its reason, always. */
    enum Decision {
        UPHELD,
        DISMISSED
    }

    protected Report() {
    }

    Report(long reporterId, TargetKind targetKind, long targetId,
           ReportCategory category, String account, Instant now) {
        this.reporterId = reporterId;
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.category = category;
        this.account = account;
        this.createdAt = now;
        this.acknowledgedAt = now;
    }

    Long id() {
        return id;
    }

    long reporterId() {
        return reporterId;
    }

    TargetKind targetKind() {
        return targetKind;
    }

    long targetId() {
        return targetId;
    }

    ReportCategory category() {
        return category;
    }

    String account() {
        return account;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant decidedAt() {
        return decidedAt;
    }

    Decision decision() {
        return decision;
    }

    String decisionReason() {
        return decisionReason;
    }

    boolean open() {
        return decidedAt == null;
    }

    /**
     * Record the decision and its reason (§10.1: "decision and reason recorded").
     * Idempotent in the same shape as every other dated fact in the product: the first
     * decision stands, so a double-submitted queue form cannot rewrite history.
     */
    void decide(Decision decision, String reason, long decidedBy, Instant now) {
        if (decidedAt == null) {
            this.decision = decision;
            this.decisionReason = reason;
            this.decidedBy = decidedBy;
            this.decidedAt = now;
        }
    }
}
