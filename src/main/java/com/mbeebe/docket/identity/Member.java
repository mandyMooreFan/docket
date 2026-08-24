package com.mbeebe.docket.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The Member is the account, never public (CONTEXT.md). Carries the Age fact in its
 * minimal form (SPEC.md §9.3): an adult is only "adult, declared on a date" — birth
 * month/year exist solely on minors, solely to drive the automatic 18 rollover.
 */
@Entity
@Table(name = "member")
public class Member {

    public enum AgeKind { ADULT, MINOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_kind")
    private AgeKind ageKind;

    private LocalDate ageDeclaredOn;

    private Integer birthMonth;

    private Integer birthYear;

    /** §11.2: the dated fact that this Member ended. Null for every live account. */
    private Instant terminatedAt;

    private String terminationReason;

    protected Member() {
    }

    private Member(String email, Instant createdAt, AgeKind ageKind,
                   LocalDate ageDeclaredOn, Integer birthMonth, Integer birthYear) {
        this.email = email;
        this.createdAt = createdAt;
        this.ageKind = ageKind;
        this.ageDeclaredOn = ageDeclaredOn;
        this.birthMonth = birthMonth;
        this.birthYear = birthYear;
    }

    static Member adult(String email, LocalDate declaredOn, Instant now) {
        return new Member(email, now, AgeKind.ADULT, declaredOn, null, null);
    }

    static Member minor(String email, YearMonth birth, LocalDate declaredOn, Instant now) {
        return new Member(email, now, AgeKind.MINOR, declaredOn,
                birth.getMonthValue(), birth.getYear());
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public boolean isMinor() {
        return ageKind == AgeKind.MINOR;
    }

    public AgeKind ageKind() {
        return ageKind;
    }

    public LocalDate ageDeclaredOn() {
        return ageDeclaredOn;
    }

    /** Only meaningful for a minor; an adult's birth data does not exist (§9.3). */
    YearMonth birth() {
        return YearMonth.of(birthYear, birthMonth);
    }

    boolean dueForRollover(YearMonth now) {
        return isMinor() && !terminated() && AgeRules.reached(18, birth(), now);
    }

    /** The 18 rollover: collapse to the adult fact and delete the birth data (§9.3). */
    void rolloverToAdult() {
        this.ageKind = AgeKind.ADULT;
        this.birthMonth = null;
        this.birthYear = null;
    }

    /**
     * §11.2: whether this Member ended. Everything a live Member has — a Profile,
     * a place in search, a face at /images/{id}, a session — is withheld on this
     * one fact, derived at the point of asking rather than mirrored into a flag on
     * every table (ADR-0002).
     */
    public boolean terminated() {
        return terminatedAt != null;
    }

    /**
     * The end of a Member (CONTEXT.md, §11.2), as far as identity is concerned:
     * dated, reasoned, and stripped.
     *
     * <p>The row itself stays, and has to — V8's thread and message references
     * deliberately do not cascade, so deleting it would be refused, and making
     * them cascade would let one person delete another's correspondence. What
     * leaves is everything that identified the person: the email is replaced with
     * a value that identifies nobody and unblocks the unique index (they must be
     * able to join again from scratch), and a minor's birth month/year go the way
     * the 18 rollover sends them (§9.3) — held for exactly one purpose, and that
     * purpose has ended. {@code ageKind} is deliberately NOT flipped to ADULT: the
     * rollover may say that truthfully, this may not.
     */
    void terminate(String reason, Instant now) {
        if (terminatedAt != null) {
            return;
        }
        this.terminatedAt = now;
        this.terminationReason = reason;
        this.email = "former-member-" + id + "@docket.invalid";
        this.birthMonth = null;
        this.birthYear = null;
    }
}
