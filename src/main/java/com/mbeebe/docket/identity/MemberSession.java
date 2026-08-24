package com.mbeebe.docket.identity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/** A 90-day sliding session (§3.3) — enumerable so the settings list can show it. */
@Entity
@Table(name = "member_session")
class MemberSession {

    static final Duration LIFETIME = Duration.ofDays(90);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    private Instant createdAt;

    private Instant lastUsedAt;

    private String client;

    protected MemberSession() {
    }

    MemberSession(String tokenHash, Member member, Instant now, String client) {
        this.tokenHash = tokenHash;
        this.member = member;
        this.createdAt = now;
        this.lastUsedAt = now;
        this.client = client;
    }

    Long id() {
        return id;
    }

    String tokenHash() {
        return tokenHash;
    }

    Member member() {
        return member;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant lastUsedAt() {
        return lastUsedAt;
    }

    String client() {
        return client;
    }

    boolean alive(Instant now) {
        return now.isBefore(lastUsedAt.plus(LIFETIME));
    }

    /** The slide: every authenticated request pushes the 90 days forward. */
    void touch(Instant now) {
        this.lastUsedAt = now;
    }
}
