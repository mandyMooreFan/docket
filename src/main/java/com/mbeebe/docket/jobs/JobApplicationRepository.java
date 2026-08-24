package com.mbeebe.docket.jobs;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface JobApplicationRepository extends Repository<JobApplication, Long> {

    JobApplication save(JobApplication application);

    Optional<JobApplication> findById(long id);

    Optional<JobApplication> findByPostingIdAndApplicantId(long postingId, long applicantId);

    List<JobApplication> findByPostingIdOrderByAppliedAtAscIdAsc(long postingId);

    List<JobApplication> findByApplicantIdOrderByAppliedAtDescIdDesc(long applicantId);

    /**
     * §6.4's block, derived at the point of asking (ADR-0002): the Applications
     * on this poster's postings that closed without response and still hold no
     * poster-made Outcome. The obligation follows the person, not the posting.
     */
    @Query("select a from JobApplication a, JobPosting p "
            + "where a.postingId = p.id and p.posterId = :posterId "
            + "and a.closedWithoutResponseAt is not null and a.outcome is null")
    List<JobApplication> neglectedOnPostingsOf(@Param("posterId") long posterId);
}
