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
 * An intimate image content report (§10.5, OSA s.20A) — a distinct route, no account
 * required, because s.20A covers "users <em>and affected persons</em>" and the person
 * depicted is precisely the one least likely to hold an account.
 *
 * <p>s.20A(2)'s prescribed contents are held by check constraints in the schema, not by
 * form validation: a row that does not carry the subject-or-acting-for declaration and
 * the good-faith statement cannot exist. That is the difference between a rule the code
 * follows and a rule the data obeys.
 *
 * <p>The locator is deliberately generous free text. §10.5 accepts an imprecise
 * location, because a non-member cannot see a private Thread and demanding a URL they
 * cannot obtain would close the route to exactly the people it exists for.
 */
@Entity
@Table(name = "intimate_image_report")
class IntimateImageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String locator;

    private boolean subjectDeclared;

    private boolean actingFor;

    private boolean goodFaith;

    private String contact;

    private String requestIp;

    private Instant createdAt;

    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    private String decisionReason = "";

    /** What the human review concluded — the hold either becomes permanent or is undone. */
    enum Outcome {
        /** The content was what it was reported to be; the removal stands. */
        CONFIRMED,
        /** It was not; the hold is released and the content comes back. */
        RESTORED
    }

    protected IntimateImageReport() {
    }

    IntimateImageReport(String locator, boolean subjectDeclared, boolean actingFor,
                        boolean goodFaith, String contact, String requestIp, Instant now) {
        this.locator = locator;
        this.subjectDeclared = subjectDeclared;
        this.actingFor = actingFor;
        this.goodFaith = goodFaith;
        this.contact = contact;
        this.requestIp = requestIp;
        this.createdAt = now;
    }

    Long id() {
        return id;
    }

    String locator() {
        return locator;
    }

    String contact() {
        return contact;
    }

    Instant createdAt() {
        return createdAt;
    }

    Outcome outcome() {
        return outcome;
    }

    boolean open() {
        return decidedAt == null;
    }

    void decide(Outcome outcome, String reason, Instant now) {
        if (decidedAt == null) {
            this.outcome = outcome;
            this.decisionReason = reason;
            this.decidedAt = now;
        }
    }
}
