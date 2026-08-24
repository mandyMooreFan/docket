package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Work verification (CONTEXT.md, §6.2, §16): a dated fact that a Member could
 * receive mail at one of a Company's domains. It records a moment and never lapses —
 * there is deliberately no expiry and no revocation. Only the domain is kept: the
 * address that proved it was operational, not a fact worth retaining (§9.3's spirit).
 * The Verified domain set is derived from these rows and never declared (ADR-0002).
 */
@Entity
@Table(name = "work_verification")
class WorkVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long companyId;

    private String domain;

    private Instant verifiedAt;

    protected WorkVerification() {
    }

    WorkVerification(long memberId, long companyId, String domain, Instant verifiedAt) {
        this.memberId = memberId;
        this.companyId = companyId;
        this.domain = domain;
        this.verifiedAt = verifiedAt;
    }

    Long id() {
        return id;
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

    /** A merge repoints the fact to the surviving Company (§10.5). */
    void repointTo(long companyId) {
        this.companyId = companyId;
    }
}
