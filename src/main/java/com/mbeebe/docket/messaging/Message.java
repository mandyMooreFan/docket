package com.mbeebe.docket.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Message (SPEC.md §7.2, CONTEXT.md): one entry in a Thread — text, links and
 * still images, nothing else. Immutable once written: neither person may edit
 * or destroy the other's record of the correspondence (§7.3, §11.1), which is
 * also why a Message row is never deleted when a Connection ends.
 */
@Entity
@Table(name = "message")
class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long threadId;

    private Long authorId;

    private String body;

    /**
     * §10.3 rung 1, in {@code reply.removed_at}'s shape. Immutability binds the
     * two correspondents, not moderation: neither may edit or destroy the other's
     * record, and illegal content in a private Thread is still illegal content.
     * No member-facing route sets this.
     */
    private Instant removedAt;

    private Instant createdAt;

    protected Message() {
    }

    Message(long threadId, long authorId, String body, Instant createdAt) {
        this.threadId = threadId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
    }

    long id() {
        return id;
    }

    long threadId() {
        return threadId;
    }

    long authorId() {
        return authorId;
    }

    String body() {
        return body;
    }

    Instant createdAt() {
        return createdAt;
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
     * a Message that is not removed is left alone.
     */
    void restore() {
        if (removedAt != null) {
            removedAt = null;
        }
    }
}
