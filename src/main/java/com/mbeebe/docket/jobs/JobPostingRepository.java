package com.mbeebe.docket.jobs;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface JobPostingRepository extends Repository<JobPosting, Long> {

    JobPosting save(JobPosting posting);

    Optional<JobPosting> findById(long id);

    /**
     * §6.5: one list, newest first — open means the window edge is still ahead
     * of the clock and no sweep has recorded a close. Every filter narrows this.
     */
    @Query("select p from JobPosting p where p.closedAt is null and p.closesAt > :now "
            + "order by p.postedAt desc, p.id desc")
    List<JobPosting> openAt(@Param("now") Instant now);

    @Query("select p from JobPosting p where p.companyId = :companyId "
            + "and p.closedAt is null and p.closesAt > :now "
            + "order by p.postedAt desc, p.id desc")
    List<JobPosting> openAtCompany(@Param("companyId") long companyId, @Param("now") Instant now);

    /** The sweep's worklist: past the window's edge, not yet closed (§6.4). */
    @Query("select p from JobPosting p where p.closedAt is null and p.closesAt <= :now")
    List<JobPosting> dueToClose(@Param("now") Instant now);

    List<JobPosting> findByPosterIdOrderByPostedAtDesc(long posterId);
}
