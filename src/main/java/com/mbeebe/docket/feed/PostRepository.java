package com.mbeebe.docket.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * One author's standing Posts, newest first. §10.3's removal is a predicate
     * here rather than a filter at each call site, so a new caller inherits it.
     */
    List<Post> findByAuthorIdAndRemovedAtIsNullOrderByCreatedAtDescIdDesc(long authorId);

    /**
     * The feed's whole query (§5.1): the mutual graph and nothing else, strictly
     * after the read position, newest first. Ids break timestamp ties so the
     * order is total and the high-water mark never lets a Post show twice. A
     * removed Post (§10.3) never enters it.
     */
    List<Post> findByAuthorIdInAndRemovedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            Collection<Long> authorIds, Instant after);

    /**
     * §8's post group: body matches, ranked by the text and tied on the id
     * (§8.2). Candidates only — who may read each one is
     * {@link PostService#visibleTo}'s answer, derived after the match, so the
     * author's Dial and §9.4's permanent cap are never baked into the index.
     * Removal (§10.3) is a predicate rather than a later filter — a removed Post
     * is not a candidate at all.
     */
    @Query(value = """
            select p.* from post p
            where p.removed_at is null
              and p.body_tsv @@ to_tsquery('english'::regconfig, cast(:tsquery as text))
            order by ts_rank(p.body_tsv, to_tsquery('english'::regconfig, cast(:tsquery as text))) desc,
                     p.id asc
            limit :limit
            """, nativeQuery = true)
    List<Post> bodyCandidates(@Param("tsquery") String tsquery, @Param("limit") int limit);
}
