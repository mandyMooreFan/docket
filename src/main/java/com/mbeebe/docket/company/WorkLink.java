package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One verification link actually sent (§6.2) — the same shape as identity's
 * magic_link. The address exists to send the mail and is blanked at consumption:
 * the durable {@link WorkVerification} keeps only the domain.
 */
@Entity
@Table(name = "work_link")
class WorkLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenHash;

    private Long memberId;

    private Long companyId;

    private String domain;

    private String address;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant usedAt;

    protected WorkLink() {
    }

    WorkLink(String tokenHash, long memberId, long companyId, String domain, String address,
             Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.memberId = memberId;
        this.companyId = companyId;
        this.domain = domain;
        this.address = address;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    Long memberId() {
        return memberId;
    }

    Long companyId() {
        return companyId;
    }

    String domain() {
        return domain;
    }

    boolean usable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /** Consuming spends the link and drops the address it no longer needs. */
    void markUsed(Instant now) {
        this.usedAt = now;
        this.address = null;
    }
}
