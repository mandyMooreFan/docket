package com.mbeebe.docket.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The auto-hide (§10.4.3, §10.5): a content-level, reversible, pre-decision hold that
 * sits deliberately <em>outside</em> the four-rung ladder and implies no finding
 * against the uploader. Its own table for exactly that reason — a hold recorded as a
 * moderation action would show up in the transparency log as something done to a
 * member, which it is not.
 *
 * <p>This is where s.10(3A)'s 48-hour clock is answered, and the answer is that there
 * is no clock. The hold is written in the same transaction that receives the report, so
 * the product has no state in which reported content is visible and a deadline is
 * running. Nothing here is a due date, because nothing is due.
 *
 * <p>Not shadowbanning, and the difference is disclosure: one item, visibly gone, told
 * to both parties, reversible. §10.3 refuses covert reach reduction; this is the
 * opposite of covert.
 */
@Entity
@Table(name = "content_hold")
class ContentHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TargetKind targetKind;

    private Long targetId;

    private Long reportId;

    private Instant heldAt;

    private Instant releasedAt;

    protected ContentHold() {
    }

    ContentHold(TargetKind targetKind, long targetId, long reportId, Instant now) {
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.reportId = reportId;
        this.heldAt = now;
    }

    Long id() {
        return id;
    }

    TargetKind targetKind() {
        return targetKind;
    }

    long targetId() {
        return targetId;
    }

    long reportId() {
        return reportId;
    }

    Instant heldAt() {
        return heldAt;
    }

    boolean live() {
        return releasedAt == null;
    }

    /** Undo the hold. The content comes back exactly as it was; nothing was destroyed. */
    void release(Instant now) {
        if (releasedAt == null) {
            releasedAt = now;
        }
    }
}
