package com.mbeebe.docket.jobs;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface JobSearchRepository extends Repository<JobSearch, Long> {

    JobSearch save(JobSearch search);

    Optional<JobSearch> findByStopToken(String stopToken);

    Optional<JobSearch> findByIdAndMemberId(long id, long memberId);

    List<JobSearch> findByMemberIdAndStoppedAtIsNullOrderByCreatedAt(long memberId);

    List<JobSearch> findByStoppedAtIsNull();
}
