package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ModerationActionRepository extends Repository<ModerationAction, Long> {

    ModerationAction save(ModerationAction action);

    Optional<ModerationAction> findById(long id);

    /**
     * Every rung standing against a Member. Kept deliberately coarse — the caller
     * filters by {@link ModerationAction#inForceAt} rather than the query, because
     * "still in force" is a derived conclusion about an instant and belongs in the
     * domain, not in a predicate that would silently disagree with it (ADR-0002).
     */
    List<ModerationAction> findByMemberIdAndReversedAtIsNull(long memberId);

    List<ModerationAction> findByMemberIdOrderByActedAtDesc(long memberId);

    List<ModerationAction> findByReportIdOrderByIdAsc(long reportId);

    long countByActedAtAfterAndKind(Instant since, ModerationAction.Kind kind);
}
