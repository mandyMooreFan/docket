package com.mbeebe.docket.invites;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The Invite (CONTEXT.md, SPEC.md §13.3): an offer sent to an email address that
 * belongs to no Member yet, carrying an optional note. It never gates signup; it
 * becomes a Connection request if the person joins.
 *
 * <p>Every accepted attempt is a row here, mailed or not. This table is both the
 * record and §3.3's rate-limit ledger, in identity's {@code link_request} shape
 * and for identity's reason: limits that counted only real sends would answer
 * differently for an address that already has an account, and that difference is
 * §8.3's membership oracle by another door.
 *
 * <p><strong>Deliberately not stored: whether mail actually went.</strong> The
 * only thing that fact records is the answer to "was that address already a
 * Member", which is the question §8.3 refuses to answer. Nothing in the product
 * needs it, so it does not exist.
 *
 * <p><strong>Deliberately not stored either: what became of the waiting
 * request.</strong> §9.2 can refuse it, and a row saying so would be exactly the
 * age fact an adult inviter must never learn about the person they invited.
 * {@code landedAt} says only that this Invite has been spent, so it cannot be
 * spent twice; the sender is never shown it.
 */
@Entity
@Table(name = "invite")
class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;

    private String email;

    private String note;

    private Instant sentAt;

    private Instant landedAt;

    protected Invite() {
    }

    Invite(long senderId, String email, String note, Instant sentAt) {
        this.senderId = senderId;
        this.email = email;
        this.note = note;
        this.sentAt = sentAt;
    }

    long id() {
        return id;
    }

    long senderId() {
        return senderId;
    }

    String email() {
        return email;
    }

    String note() {
        return note;
    }

    Instant sentAt() {
        return sentAt;
    }

    /** Spent: the invitee joined, and whatever was going to happen has happened. */
    void land(Instant now) {
        this.landedAt = now;
    }
}
