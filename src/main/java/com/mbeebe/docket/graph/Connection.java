package com.mbeebe.docket.graph;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The Connection fact itself, stored per §16 (§4.2 defines it): one row per pair,
 * lower member id first. Acceptance creates it; Disconnect or Block deletes it —
 * the row ending IS the quiet severance, and reconnecting is simply a new row.
 */
@Entity
@Table(name = "connection")
class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Explicit names: the naming strategy would not underscore a trailing capital.
    @Column(name = "member_a")
    private Long memberA;

    @Column(name = "member_b")
    private Long memberB;

    private Instant connectedAt;

    protected Connection() {
    }

    Connection(long one, long other, Instant connectedAt) {
        this.memberA = Math.min(one, other);
        this.memberB = Math.max(one, other);
        this.connectedAt = connectedAt;
    }

    Instant connectedAt() {
        return connectedAt;
    }

    /** The far end of the Connection, seen from one of its members. */
    long other(long memberId) {
        return memberA == memberId ? memberB : memberA;
    }
}
