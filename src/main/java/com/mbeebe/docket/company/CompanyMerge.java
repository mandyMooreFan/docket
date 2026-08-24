package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A merge, recorded as a fact (§10.5): privileged, audited, reversible — a
 * destructive action on other people's employment history keeps its full record.
 * SHARED_DOMAIN is §6.1's auto-merge (no actor: the facts themselves compelled it);
 * MANUAL carries the acting moderator and arrives with #38, as does reversal.
 */
@Entity
@Table(name = "company_merge")
class CompanyMerge {

    enum Cause { SHARED_DOMAIN, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long absorbedCompanyId;

    private Long survivingCompanyId;

    @Enumerated(EnumType.STRING)
    private Cause cause;

    private Long actorMemberId;

    private Instant mergedAt;

    private Instant reversedAt;

    protected CompanyMerge() {
    }

    CompanyMerge(long absorbedCompanyId, long survivingCompanyId, Cause cause,
                 Long actorMemberId, Instant mergedAt) {
        this.absorbedCompanyId = absorbedCompanyId;
        this.survivingCompanyId = survivingCompanyId;
        this.cause = cause;
        this.actorMemberId = actorMemberId;
        this.mergedAt = mergedAt;
    }

    Long id() {
        return id;
    }
}
