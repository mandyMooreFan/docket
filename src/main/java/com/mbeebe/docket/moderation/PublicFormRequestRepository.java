package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.time.Instant;

interface PublicFormRequestRepository extends Repository<PublicFormRequest, Long> {

    PublicFormRequest save(PublicFormRequest request);

    long countByFormAndContactAndCreatedAtAfter(
            PublicFormRequest.Form form, String contact, Instant cutoff);

    long countByFormAndRequestIpAndCreatedAtAfter(
            PublicFormRequest.Form form, String requestIp, Instant cutoff);
}
