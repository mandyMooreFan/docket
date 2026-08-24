package com.mbeebe.docket.identity;

import org.springframework.data.repository.Repository;

import java.time.Instant;

interface LinkRequestRepository extends Repository<LinkRequest, Long> {

    LinkRequest save(LinkRequest request);

    long countByEmailAndCreatedAtAfter(String email, Instant cutoff);

    long countByRequestIpAndCreatedAtAfter(String requestIp, Instant cutoff);
}
