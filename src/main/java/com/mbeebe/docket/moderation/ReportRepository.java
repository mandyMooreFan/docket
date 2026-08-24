package com.mbeebe.docket.moderation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ReportRepository extends Repository<Report, Long> {

    Report save(Report report);

    Optional<Report> findById(long id);

    /** The queue (§10.1): open Reports, oldest first, because a queue is not a ranking. */
    List<Report> findByDecidedAtIsNullOrderByCreatedAtAscIdAsc();

    List<Report> findByReporterIdOrderByCreatedAtDesc(long reporterId);

    long countByDecidedAtIsNull();

    /**
     * The transparency log (§10.3): Reports received and actioned, by category, no
     * names. Grouped in the database precisely so nothing member-shaped is loaded to
     * produce a public page.
     */
    @Query("""
            select r.category as category,
                   count(r) as received,
                   sum(case when r.decision = com.mbeebe.docket.moderation.Report$Decision.UPHELD
                            then 1 else 0 end) as upheld,
                   sum(case when r.decision = com.mbeebe.docket.moderation.Report$Decision.DISMISSED
                            then 1 else 0 end) as dismissed
            from Report r
            where r.createdAt >= :since
            group by r.category
            """)
    List<CategoryCount> countsByCategorySince(@Param("since") Instant since);

    /** One row of the transparency log. Counts only — the projection cannot carry a name. */
    interface CategoryCount {
        ReportCategory getCategory();

        long getReceived();

        long getUpheld();

        long getDismissed();
    }
}
