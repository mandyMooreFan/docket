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
import java.time.YearMonth;

/**
 * One row per link requested — login and verification are the same mechanism (§3.1).
 * A JOIN link carries the declared age fact forward to account creation; these rows
 * are also the ledger the §3.3 rate limits count against.
 */
@Entity
@Table(name = "magic_link")
class MagicLink {

    enum Purpose { JOIN, LOGIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenHash;

    private String email;

    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_kind")
    private Member.AgeKind ageKind;

    private Integer birthMonth;

    private Integer birthYear;

    private String requestIp;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant usedAt;

    protected MagicLink() {
    }

    private MagicLink(String tokenHash, String email, Purpose purpose, Member.AgeKind ageKind,
                      Integer birthMonth, Integer birthYear, String requestIp,
                      Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.purpose = purpose;
        this.ageKind = ageKind;
        this.birthMonth = birthMonth;
        this.birthYear = birthYear;
        this.requestIp = requestIp;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    static MagicLink join(String tokenHash, String email, Member.AgeKind ageKind,
                          YearMonth birthOrNull, String requestIp, Instant now, Instant expiresAt) {
        return new MagicLink(tokenHash, email, Purpose.JOIN, ageKind,
                birthOrNull == null ? null : birthOrNull.getMonthValue(),
                birthOrNull == null ? null : birthOrNull.getYear(),
                requestIp, now, expiresAt);
    }

    static MagicLink login(String tokenHash, String email, String requestIp,
                           Instant now, Instant expiresAt) {
        return new MagicLink(tokenHash, email, Purpose.LOGIN, null, null, null,
                requestIp, now, expiresAt);
    }

    String email() {
        return email;
    }

    Purpose purpose() {
        return purpose;
    }

    Member.AgeKind ageKind() {
        return ageKind;
    }

    YearMonth birth() {
        return YearMonth.of(birthYear, birthMonth);
    }

    boolean usable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }
}
