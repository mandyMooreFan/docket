package com.mbeebe.docket.search;

import org.springframework.data.repository.Repository;

import java.time.Instant;

interface SearchRequestRepository extends Repository<SearchRequest, Long> {

    SearchRequest save(SearchRequest request);

    long countByMemberIdAndCreatedAtAfter(long memberId, Instant cutoff);

    long countByRequestIpAndCreatedAtAfter(String requestIp, Instant cutoff);
}
