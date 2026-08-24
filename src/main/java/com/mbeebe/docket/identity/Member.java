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
        return isMinor() && AgeRules.reached(18, birth(), now);
    }

    /** The 18 rollover: collapse to the adult fact and delete the birth data (§9.3). */
    void rolloverToAdult() {
        this.ageKind = AgeKind.ADULT;
        this.birthMonth = null;
        this.birthYear = null;
    }
}
