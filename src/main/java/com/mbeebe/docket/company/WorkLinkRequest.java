package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The rate-limit ledger for verification sends — identity's link_request pattern
 * (§3.3, §8.3): one row per accepted request, so the limiter's behaviour never
 * depends on what exists at the other end.
 */
@Entity
@Table(name = "work_link_request")
class WorkLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private String address;

    private Instant createdAt;

    protected WorkLinkRequest() {
    }

    WorkLinkRequest(long memberId, String address, Instant createdAt) {
        this.memberId = memberId;
        this.address = address;
        this.createdAt = createdAt;
    }
}
