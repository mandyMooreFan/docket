package com.mbeebe.docket.graph;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Connection request (CONTEXT.md): the offer of a Connection, its optional note,
 * and what became of it (SPEC.md §4.2). Rows are never deleted: a DECLINED row is
 * the fact that blocks repeat requests, while the sender's view keeps deriving
 * "sent" from it — decline stays silent because nothing the sender can observe
 * ever changes.
 */
@Entity
@Table(name = "connection_request")
class ConnectionRequest {

    enum State { PENDING, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requesterId;

    private Long recipientId;

    private String note;

    @Enumerated(EnumType.STRING)
    private State state;

    private Instant sentAt;

    private Instant respondedAt;

    protected ConnectionRequest() {
    }

    ConnectionRequest(long requesterId, long recipientId, String note, Instant sentAt) {
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.note = note;
        this.state = State.PENDING;
        this.sentAt = sentAt;
    }

    long requesterId() {
        return requesterId;
    }

    long recipientId() {
        return recipientId;
    }

    State state() {
        return state;
    }

    Instant sentAt() {
        return sentAt;
    }

    String note() {
        return note;
    }

    void accept(Instant now) {
        this.state = State.ACCEPTED;
        this.respondedAt = now;
    }

    /** Silent (§4.2): records the fact, and nothing the requester can observe. */
    void decline(Instant now) {
        this.state = State.DECLINED;
        this.respondedAt = now;
    }
}
