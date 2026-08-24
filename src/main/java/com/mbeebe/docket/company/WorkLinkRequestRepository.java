package com.mbeebe.docket.company;

import org.springframework.data.repository.Repository;

import java.time.Instant;

interface WorkLinkRequestRepository extends Repository<WorkLinkRequest, Long> {

    WorkLinkRequest save(WorkLinkRequest request);

    long countByMemberIdAndCreatedAtAfter(long memberId, Instant cutoff);

    long countByAddressAndCreatedAtAfter(String address, Instant cutoff);
}
