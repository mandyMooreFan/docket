package com.mbeebe.docket.company;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface WorkVerificationRepository extends Repository<WorkVerification, Long> {

    WorkVerification save(WorkVerification verification);

    List<WorkVerification> findByMemberId(long memberId);

    List<WorkVerification> findByCompanyId(long companyId);

    /** The Verified domain set of a Company — always derived, never declared. */
    @Query("select distinct w.domain from WorkVerification w where w.companyId = :companyId")
    List<String> domainsOf(@Param("companyId") long companyId);

    /** Every Company currently demonstrated to hold this domain. */
    @Query("select distinct w.companyId from WorkVerification w where w.domain = :domain")
    List<Long> companyIdsHolding(@Param("domain") String domain);
}
