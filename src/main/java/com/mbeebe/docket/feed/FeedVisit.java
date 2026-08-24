package com.mbeebe.docket.feed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The per-member read position (§5.1's tracked state): one high-water mark.
 * It only ever moves forward, which is what makes "nothing re-surfaces,
 * nothing shows twice" structural rather than hopeful.
 */
@Entity
@Table(name = "feed_visit")
class FeedVisit {

    @Id
    private Long memberId;

    private Instant seenUpTo;

    protected FeedVisit() {
    }

    FeedVisit(long memberId, Instant seenUpTo) {
        this.memberId = memberId;
        this.seenUpTo = seenUpTo;
    }

    Instant seenUpTo() {
        return seenUpTo;
    }

    void advanceTo(Instant instant) {
        if (instant.isAfter(seenUpTo)) {
            seenUpTo = instant;
        }
    }
}
