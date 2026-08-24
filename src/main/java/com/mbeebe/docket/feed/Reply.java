package com.mbeebe.docket.feed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Reply (§5.3, CONTEXT.md): a response to a Post by one of the Post author's
 * Connections. Not a Post — it never enters a feed. Carries the same immutable
 * §9.4 authored-as-minor fact; a Reply inherits its author's protection, not
 * the Post's audience. {@code removedAt} is the Post author's removal, stored
 * as a fact — removed Replies stop rendering and stop counting.
 */
@Entity
@Table(name = "reply")
class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;

    private Long authorId;

    private String body;

    /** §9.4: immutable at creation; the 18 rollover never lifts this. */
    private boolean authoredAsMinor;

    private Instant removedAt;

    private Instant createdAt;

    protected Reply() {
    }

    Reply(long postId, long authorId, String body, boolean authoredAsMinor, Instant createdAt) {
        this.postId = postId;
        this.authorId = authorId;
        this.body = body;
        this.authoredAsMinor = authoredAsMinor;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    long postId() {
        return postId;
    }

    long authorId() {
        return authorId;
    }

    String body() {
        return body;
    }

    boolean authoredAsMinor() {
        return authoredAsMinor;
    }

    Instant createdAt() {
        return createdAt;
    }

    boolean removed() {
        return removedAt != null;
    }

    void remove(Instant now) {
        if (removedAt == null) {
            removedAt = now;
        }
    }

    /**
     * §10.5: the dated fact lifted, the item back exactly as it was. Idempotent —
     * a Reply that is not removed is left alone.
     */
    void restore() {
        if (removedAt != null) {
            removedAt = null;
        }
    }
}
