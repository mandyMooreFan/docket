package com.mbeebe.docket.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByPostIdAndRemovedAtIsNullOrderByCreatedAtAscIdAsc(long postId);

    /** §5.5's raw material: fresh, unremoved replies across a set of threads. */
    List<Reply> findByPostIdInAndRemovedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            Collection<Long> postIds, Instant after);

    /** The threads this member has joined (§5.5) — their unremoved replies mark them. */
    @Query("select distinct r.postId from Reply r"
            + " where r.authorId = :memberId and r.removedAt is null")
    List<Long> postIdsJoinedBy(@Param("memberId") long memberId);
}
