package com.mbeebe.docket.jobs;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An Application (SPEC.md §6.3, CONTEXT.md): a Member offering their Profile to
 * a Job posting — one click plus an optional note; there is nothing else to
 * send. The Outcome (§6.4) is the poster's dated decision, stored apart from
 * {@code closedWithoutResponseAt} — the sweep's immutable record that the
 * posting closed while this Application was untouched — so a late resolution
 * never erases the fact of the silence. The state the applicant sees is always
 * derived from these facts (ADR-0002). The entity is named JobApplication only
 * because the domain word collides with Spring's Application class; the table
 * and the language stay "application".
 */
@Entity
@Table(name = "application")
class JobApplication {

    /** The poster's Outcome (CONTEXT.md): advanced, or not selected. */
    enum Outcome { ADVANCED, NOT_SELECTED }

    /** The derived state the applicant always sees (§6.4). */
    enum State { RECEIVED, ADVANCED, NOT_SELECTED, CLOSED_WITHOUT_RESPONSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postingId;

    private Long applicantId;

    private String note;

    private Instant appliedAt;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    private Instant outcomeAt;

    private Instant closedWithoutResponseAt;

    protected JobApplication() {
    }

    JobApplication(long postingId, long applicantId, String note, Instant appliedAt) {
        this.postingId = postingId;
        this.applicantId = applicantId;
        this.note = note;
        this.appliedAt = appliedAt;
    }

    Long id() {
        return id;
    }

    long postingId() {
        return postingId;
    }

    long applicantId() {
        return applicantId;
    }

    String note() {
        return note;
    }

    Instant appliedAt() {
        return appliedAt;
    }

    Outcome outcome() {
        return outcome;
    }

    Instant outcomeAt() {
        return outcomeAt;
    }

    Instant closedWithoutResponseAt() {
        return closedWithoutResponseAt;
    }

    State state() {
        if (outcome == Outcome.ADVANCED) {
            return State.ADVANCED;
        }
        if (outcome == Outcome.NOT_SELECTED) {
            return State.NOT_SELECTED;
        }
        return closedWithoutResponseAt != null
                ? State.CLOSED_WITHOUT_RESPONSE
                : State.RECEIVED;
    }

    /** §6.4: the obligation still owed a person — no poster-made Outcome yet. */
    boolean unresolved() {
        return outcome == null;
    }

    /** An Outcome is a dated fact: set once, never rewritten. True when it landed. */
    boolean resolve(Outcome decision, Instant now) {
        if (outcome != null) {
            return false;
        }
        outcome = decision;
        outcomeAt = now;
        return true;
    }

    /** The sweep's record (§6.4): closed while untouched. Immutable once set. */
    void closeWithoutResponse(Instant now) {
        if (outcome == null && closedWithoutResponseAt == null) {
            closedWithoutResponseAt = now;
        }
    }
}
