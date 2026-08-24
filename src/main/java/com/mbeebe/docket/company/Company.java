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
}
