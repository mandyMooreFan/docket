package com.mbeebe.docket.jobs;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /**
     * §7.1's other gate, derived at the point of asking (ADR-0002): is any
     * Application between these two Members still running, whichever of them
     * posted? The window is deliberately the Application's own life, not the
     * posting's:
     *
     * <ul>
     *   <li>from the moment of applying, while the posting's window is open —
     *       the poster works a queue, and a reply may take a week (§6.4, §7.4);
     *   <li>indefinitely once <strong>advanced</strong> — advancing is the
     *       start of a conversation, not the end of one, and §7.4 spends its
     *       one accepted cost on that reply arriving only in the inbox;
     *   <li>ended by <strong>not selected</strong>, and by the §6.4 close
     *       without response — both are the Application's ending, and both are
     *       carried to the applicant by the transactional mail §6.5 allows.
     * </ul>
     *
     * <p>The posting's window edge is read against the clock rather than
     * waiting on {@code closedAt}, so the answer is the same before and after
     * the hourly sweep has run.
     */
    @Query("select count(a) from JobApplication a, JobPosting p "
            + "where a.postingId = p.id "
            + "and ((a.applicantId = :one and p.posterId = :other) "
            + "  or (a.applicantId = :other and p.posterId = :one)) "
            + "and (a.outcome = :advanced "
            + "  or (a.outcome is null and a.closedWithoutResponseAt is null "
            + "      and p.closedAt is null and p.closesAt > :now))")
    long openChannelsBetween(@Param("one") long one, @Param("other") long other,
                             @Param("advanced") JobApplication.Outcome advanced,
                             @Param("now") Instant now);
}
