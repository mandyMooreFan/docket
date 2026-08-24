package com.mbeebe.docket.identity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The rate-limit ledger (§3.3): one row per link request, sent or not — limits that
 * counted only real sends would answer differently for members and strangers, and
 * become the §8.3 membership oracle by another door.
 */
@Entity
@Table(name = "link_request")
class LinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String requestIp;

    private Instant createdAt;

    protected LinkRequest() {
    }

    LinkRequest(String email, String requestIp, Instant createdAt) {
        this.email = email;
        this.requestIp = requestIp;
        this.createdAt = createdAt;
    }
}
