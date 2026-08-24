package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface IntimateImageReportRepository extends Repository<IntimateImageReport, Long> {

    IntimateImageReport save(IntimateImageReport report);

    Optional<IntimateImageReport> findById(long id);

    /** The s.20A queue, oldest first. Separate from the ordinary one because the route is. */
    List<IntimateImageReport> findByDecidedAtIsNullOrderByCreatedAtAscIdAsc();

    long countByDecidedAtIsNull();
}
