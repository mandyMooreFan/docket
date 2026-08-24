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
}
