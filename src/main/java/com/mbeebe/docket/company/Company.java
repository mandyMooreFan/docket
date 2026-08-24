package com.mbeebe.docket.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An employer, existing because a Member named it while adding a Position (§6.1).
 * Never an actor: no account, no owner, nobody speaks for it — the page belongs to
 * the people who work there, gated by the trust gate (§6.2), and every change lands
 * in the edit history. A merged Company's row survives with a pointer to its
 * survivor, which is what keeps merges reversible and old URLs alive (§10.5).
 */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Column(name = "logo_image_id")
    private Long logoImageId;

    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    /**
     * §10.3 rung 1, in {@code reply.removed_at}'s shape — §10.3's "joke/spam
     * entities are cleaned up reactively". A removed Company stops rendering
     * everywhere; the row stays, because removal is never a delete and a merge
     * chain may still run through it.
     */
    @Column(name = "removed_at")
    private Instant removedAt;

    private Instant createdAt;

    protected Company() {
    }

    Company(String name, Instant createdAt) {
        this.name = name;
        this.description = "";
        this.createdAt = createdAt;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Long logoImageId() {
        return logoImageId;
    }

    boolean merged() {
        return mergedIntoId != null;
    }

    Long mergedIntoId() {
        return mergedIntoId;
    }

    void rename(String name) {
        this.name = name;
    }

    void describe(String description) {
        this.description = description;
    }

    void setLogo(Long imageId) {
        this.logoImageId = imageId;
    }

    /** The merge fact's pointer (§10.5): the row stays, the identity moves. */
    void markMergedInto(long survivorId) {
        this.mergedIntoId = survivorId;
    }

    /**
     * §10.3 rung 1. Public because a Company is the one reportable kind whose rows
     * other modules render directly — the Position's employer line, for one.
     */
    public boolean removed() {
        return removedAt != null;
    }

    /** §10.3 rung 1: idempotent — the first removal is the one that stands. */
    void remove(Instant now) {
        if (removedAt == null) {
            removedAt = now;
        }
    }

    /**
     * §10.5: the dated fact lifted, the Company back exactly as it was. Idempotent
     * — a Company that is not removed is left alone.
     */
    void restore() {
        if (removedAt != null) {
            removedAt = null;
        }
    }
}
