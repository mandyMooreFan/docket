package com.mbeebe.docket.graph;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Recommendation (CONTEXT.md, §4.3): the words one Member wrote about another,
 * plus two dated facts. Whether it displays is derived — approved and not since
 * hidden — never stored (ADR-0002). One per author per subject.
 */
@Entity
@Table(name = "recommendation")
class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long authorId;

    private Long subjectId;

    private String text;

    private Instant writtenAt;

    private Instant approvedAt;

    private Instant hiddenAt;

    protected Recommendation() {
    }

    Recommendation(long authorId, long subjectId, String text, Instant writtenAt) {
        this.authorId = authorId;
        this.subjectId = subjectId;
        this.text = text;
        this.writtenAt = writtenAt;
    }

    long authorId() {
        return authorId;
    }

    long subjectId() {
        return subjectId;
    }

    Instant writtenAt() {
        return writtenAt;
    }

    String text() {
        return text;
    }

    /** §4.3: it displays only after the subject approves, until they hide it. */
    boolean displayed() {
        return approvedAt != null && hiddenAt == null;
    }

    boolean awaitingApproval() {
        return approvedAt == null && hiddenAt == null;
    }

    boolean hidden() {
        return hiddenAt != null;
    }

    void approve(Instant now) {
        if (approvedAt == null) {
            approvedAt = now;
        }
    }

    void hide(Instant now) {
        if (hiddenAt == null) {
            hiddenAt = now;
        }
    }

    /** Rewriting while still unapproved replaces the words; the subject has seen nothing yet. */
    void rewrite(String text, Instant now) {
        this.text = text;
        this.writtenAt = now;
    }
}
