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
 * A Job posting (SPEC.md §6.3, CONTEXT.md): an opening authored by a Member —
 * never a Company — attached to a Company, carrying a mandatory real salary
 * range and running a fixed window. {@code closesAt} is the window's edge,
 * fixed at posting; whether the posting is open is always derived from it
 * against the clock (ADR-0002) — {@code closedAt} only records the moment the
 * closing sweep executed the §6.4 guarantee.
 */
@Entity
@Table(name = "job_posting")
class JobPosting {

    enum RemotePolicy { ON_SITE, HYBRID, REMOTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private Long posterId;

    private String title;

    private String description;

    private String location;

    @Enumerated(EnumType.STRING)
    private RemotePolicy remotePolicy;

    private Integer salaryMin;

    private Integer salaryMax;

    private String currency;

    private Instant postedAt;

    private Instant closesAt;

    private Instant closedAt;

    /**
     * §10.3 rung 1, in {@code reply.removed_at}'s shape, and deliberately NOT
     * {@code closedAt}: a closed posting is a finished one and still reads as the
     * real thing it was, while a removed posting was never legitimate and stops
     * rendering outright. Two different facts, kept apart.
     */
    private Instant removedAt;

    protected JobPosting() {
    }

    JobPosting(long companyId, long posterId, String title, String description,
               String location, RemotePolicy remotePolicy, int salaryMin, int salaryMax,
               String currency, Instant postedAt, Instant closesAt) {
        this.companyId = companyId;
        this.posterId = posterId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.remotePolicy = remotePolicy;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.currency = currency;
        this.postedAt = postedAt;
        this.closesAt = closesAt;
    }

    Long id() {
        return id;
    }

    long companyId() {
        return companyId;
    }

    long posterId() {
        return posterId;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    String location() {
        return location;
    }

    RemotePolicy remotePolicy() {
        return remotePolicy;
    }

    int salaryMin() {
        return salaryMin;
    }

    int salaryMax() {
        return salaryMax;
    }

    String currency() {
        return currency;
    }

    Instant postedAt() {
        return postedAt;
    }

    Instant closesAt() {
        return closesAt;
    }

    Instant closedAt() {
        return closedAt;
    }

    /** §6.3: a fixed window, then closed — derived, never a stored flag. */
    boolean openAt(Instant now) {
        return closedAt == null && now.isBefore(closesAt);
    }

    /** The sweep's dated fact: the moment the §6.4 guarantee was executed. */
    void markClosed(Instant now) {
        if (closedAt == null) {
            closedAt = now;
        }
    }

    boolean removed() {
        return removedAt != null;
    }

    /** §10.3 rung 1: idempotent — the first removal is the one that stands. */
    void remove(Instant now) {
        if (removedAt == null) {
            removedAt = now;
        }
    }

    /**
     * §10.5: the dated fact lifted, the item back exactly as it was. Idempotent —
     * a posting that is not removed is left alone.
     */
    void restore() {
        if (removedAt != null) {
            removedAt = null;
        }
    }
}
