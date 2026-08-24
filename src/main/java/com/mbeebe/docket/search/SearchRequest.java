package com.mbeebe.docket.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The §10.3 rate-limit ledger: one row per search that actually ran, the same
 * shape identity's link_request uses. Data-minimising by construction — a
 * signed-in search records the Member and no address; a signed-out one records
 * the address and no Member — and the schema's check constraint keeps it that
 * way, so this table can never quietly become a record of who searched from
 * where.
 */
@Entity
@Table(name = "search_request")
class SearchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "request_ip")
    private String requestIp;

    private Instant createdAt;

    protected SearchRequest() {
    }

    static SearchRequest by(long memberId, Instant createdAt) {
        SearchRequest request = new SearchRequest();
        request.memberId = memberId;
        request.createdAt = createdAt;
        return request;
    }

    static SearchRequest from(String requestIp, Instant createdAt) {
        SearchRequest request = new SearchRequest();
        request.requestIp = requestIp;
        request.createdAt = createdAt;
        return request;
    }
}
