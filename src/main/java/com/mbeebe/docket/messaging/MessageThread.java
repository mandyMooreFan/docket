package com.mbeebe.docket.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The Thread (SPEC.md §7.2, ADR-0001, CONTEXT.md): the single, permanent
 * correspondence between a pair of Members — one row per pair, ever, created
 * lazily on the first authorised write. The pair is stored lowest id first so
 * the unique index is the one-per-pair guarantee itself, not a convention
 * someone has to remember.
 *
 * <p>There is deliberately no open/closed column. Whether this Thread may be
 * written to is derived at every ask from the graph and the Applications
 * (ADR-0002), which is what makes "reconnecting reopens the same Thread" fall
 * out free. The entity is named MessageThread only because Thread collides with
 * {@link java.lang.Thread}; the table and the language stay "thread".
 */
@Entity
@Table(name = "thread")
class MessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Explicit names: the naming strategy would not underscore a trailing capital.
    @Column(name = "member_a")
    private Long memberA;

    @Column(name = "member_b")
    private Long memberB;

    private Instant createdAt;

    protected MessageThread() {
    }

    private MessageThread(long memberA, long memberB, Instant createdAt) {
        this.memberA = memberA;
        this.memberB = memberB;
        this.createdAt = createdAt;
    }

    /** The pair, normalised: lowest id first, so a pair has exactly one shape. */
    static MessageThread between(long one, long other, Instant now) {
        return new MessageThread(Math.min(one, other), Math.max(one, other), now);
    }

    long id() {
        return id;
    }

    long memberA() {
        return memberA;
    }

    long memberB() {
        return memberB;
    }

    Instant createdAt() {
        return createdAt;
    }

    long other(long memberId) {
        return memberId == memberA ? memberB : memberA;
    }

    boolean includes(long memberId) {
        return memberId == memberA || memberId == memberB;
    }
}
