package com.mbeebe.docket.jobs;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/**
 * A saved search (§6.5): the one opt-in email in the product — a filter set
 * the seeker chose, at a frequency the seeker chose, stoppable in one click.
 * {@code stopToken} is stored raw, deliberately: every send must carry the
 * stop link, a hash can only be checked, and the token authorises exactly one
 * thing — stopping this search's mail.
 */
@Entity
@Table(name = "job_search")
class JobSearch {

    enum Frequency { DAILY, WEEKLY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private String keyword;

    private String location;

    @Enumerated(EnumType.STRING)
    private JobPosting.RemotePolicy remotePolicy;

    private Integer salaryFloor;

    private String floorCurrency;

    private String company;

    private boolean knownOnly;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    private String stopToken;

    private Instant createdAt;

    private Instant lastSentAt;

    private Instant stoppedAt;

    protected JobSearch() {
    }

    JobSearch(long memberId, JobFilters filters, Integer salaryFloor,
              JobPosting.RemotePolicy remotePolicy, Frequency frequency, String stopToken,
              Instant createdAt) {
        this.memberId = memberId;
        this.keyword = filters.q();
        this.location = filters.location();
        this.remotePolicy = remotePolicy;
        this.salaryFloor = salaryFloor;
        this.floorCurrency = salaryFloor == null ? null : filters.currency();
        this.company = filters.company();
        this.knownOnly = filters.known();
        this.frequency = frequency;
        this.stopToken = stopToken;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    long memberId() {
        return memberId;
    }

    boolean knownOnly() {
        return knownOnly;
    }

    Frequency frequency() {
        return frequency;
    }

    String stopToken() {
        return stopToken;
    }

    /** The filter set, exactly as the board would read it from GET params. */
    JobFilters filters() {
        return new JobFilters(keyword, location,
                remotePolicy == null ? "" : remotePolicy.name(),
                salaryFloor == null ? "" : String.valueOf(salaryFloor),
                floorCurrency == null ? "GBP" : floorCurrency, company, knownOnly);
    }

    boolean active() {
        return stoppedAt == null;
    }

    /** Everything after this instant is news; before it was already sent. */
    Instant sentUpTo() {
        return lastSentAt == null ? createdAt : lastSentAt;
    }

    boolean due(Instant now) {
        Duration period = frequency == Frequency.DAILY ? Duration.ofDays(1) : Duration.ofDays(7);
        return active() && !now.isBefore(sentUpTo().plus(period));
    }

    void markSent(Instant now) {
        lastSentAt = now;
    }

    /** §6.5: one click to stop. A dated fact; there is no un-stop. */
    void stop(Instant now) {
        if (stoppedAt == null) {
            stoppedAt = now;
        }
    }
}
