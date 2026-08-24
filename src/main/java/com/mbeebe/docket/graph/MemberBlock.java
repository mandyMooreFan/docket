package com.mbeebe.docket.graph;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Block (CONTEXT.md, §7.3): a total, durable severance. Who blocked whom is the
 * stored fact; every conclusion drawn from it treats the pair symmetrically, and
 * v1 deliberately builds no way to lift one.
 */
@Entity
@Table(name = "member_block")
class MemberBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long blockerId;

    private Long blockedId;

    private Instant createdAt;

    protected MemberBlock() {
    }

    MemberBlock(long blockerId, long blockedId, Instant createdAt) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = createdAt;
    }
}
