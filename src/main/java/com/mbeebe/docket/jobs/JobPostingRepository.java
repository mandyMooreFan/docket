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

    /**
     * §6.5's keyword filter, as full-text rather than substring matching: the
     * same open list, narrowed by the posting's document (title, location,
     * description), and still NEWEST FIRST. The board does not rank — one list,
     * no relevance sort, no promoted postings — so the tsvector is used here
     * only to narrow, and the ts_rank the same document supports is deliberately
     * left on the table.
     */
    @Query(value = """
            select p.* from job_posting p
            where p.closed_at is null and p.closes_at > :now
              and p.text_tsv @@ to_tsquery('english'::regconfig, cast(:tsquery as text))
            order by p.posted_at desc, p.id desc
            """, nativeQuery = true)
    List<JobPosting> openMatching(@Param("tsquery") String tsquery, @Param("now") Instant now);

    /**
     * The same document, ranked — §8's jobs group, where relevance IS the order
     * (§8.2: textual match quality, tied on the id, never a function of who is
     * asking). Open only, derived from the window against the clock.
     */
    @Query(value = """
            select p.* from job_posting p
            where p.closed_at is null and p.closes_at > :now
              and p.text_tsv @@ to_tsquery('english'::regconfig, cast(:tsquery as text))
            order by ts_rank(p.text_tsv, to_tsquery('english'::regconfig, cast(:tsquery as text))) desc,
                     p.id asc
            limit :limit
            """, nativeQuery = true)
    List<JobPosting> openMatchingRanked(@Param("tsquery") String tsquery,
                                        @Param("now") Instant now, @Param("limit") int limit);

    /** The sweep's worklist: past the window's edge, not yet closed (§6.4). */
    @Query("select p from JobPosting p where p.closedAt is null and p.closesAt <= :now")
    List<JobPosting> dueToClose(@Param("now") Instant now);

    List<JobPosting> findByPosterIdOrderByPostedAtDesc(long posterId);
}
