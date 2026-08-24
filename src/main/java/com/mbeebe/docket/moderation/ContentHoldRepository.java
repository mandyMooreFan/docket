package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface ContentHoldRepository extends Repository<ContentHold, Long> {

    ContentHold save(ContentHold hold);

    Optional<ContentHold> findByTargetKindAndTargetIdAndReleasedAtIsNull(
            TargetKind targetKind, long targetId);

    boolean existsByTargetKindAndTargetIdAndReleasedAtIsNull(
            TargetKind targetKind, long targetId);

    List<ContentHold> findByReportIdOrderByIdAsc(long reportId);

    List<ContentHold> findByReleasedAtIsNull();
}
