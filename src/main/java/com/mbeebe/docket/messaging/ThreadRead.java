package com.mbeebe.docket.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * One Member's read mark in one Thread (§7.4): a high-water mark over Message
 * ids, advanced past whatever a rendering of the Thread actually showed them.
 * The Unread count is derived from it at every ask.
 *
 * <p>This row is private to its owner and is never rendered, exposed or
 * counted for anyone else: §7.2 refuses read receipts, last-seen and presence
 * outright, and the only way to keep that promise structurally is for the
 * other side's rendering to have no access to this fact at all.
 */
@Entity
@Table(name = "thread_read")
@IdClass(ThreadRead.Key.class)
class ThreadRead {

    @Id
    private Long threadId;

    @Id
    private Long memberId;

    private Long lastReadMessageId;

    protected ThreadRead() {
    }

    ThreadRead(long threadId, long memberId, long lastReadMessageId) {
        this.threadId = threadId;
        this.memberId = memberId;
        this.lastReadMessageId = lastReadMessageId;
    }

    long lastReadMessageId() {
        return lastReadMessageId;
    }

    /** A mark only ever moves forward; re-reading old Messages never un-reads new ones. */
    void advanceTo(long messageId) {
        if (messageId > lastReadMessageId) {
            lastReadMessageId = messageId;
        }
    }

    /** JPA's composite key holder for (thread, member). */
    static class Key implements Serializable {

        private Long threadId;

        private Long memberId;

        Key() {
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && Objects.equals(threadId, key.threadId)
                    && Objects.equals(memberId, key.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(threadId, memberId);
        }
    }
}
