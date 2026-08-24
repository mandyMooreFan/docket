package com.mbeebe.docket.profile;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ProfileRepository extends Repository<Profile, Long> {

    Profile save(Profile profile);

    Optional<Profile> findById(Long memberId);

    /** §11.2: your Profile goes — the row, not a blanking of its columns. */
    void delete(Profile profile);

    /**
     * Whose photo this image is, if it is anyone's current one — the lookup
     * {@link ProfilePhotoAudience} does on every /images/{id} request, which is why
     * V12 indexes the column. A photo since replaced stops being claimed and stops
     * being served, the same rule a replaced Company logo lives under.
     */
    Optional<Profile> findByPhotoImageId(Long imageId);

    /**
     * §8.1's whole matching rule: the name column against the name index, and
     * there is nothing else in the query to match on. §8.2's order —
     * ts_rank first, then the member id, so it is total, stable, and about the
     * text rather than about the asker.
     *
     * <p>Candidates only: whether the viewer may be shown any of these is
     * decided afterwards, in {@link ProfileService#searchableAmong}, never here
     * and never in the index (ADR-0002). The one exception is §10.3's removal,
     * which is a predicate here as well as an early return in
     * {@link ProfileService#pageFor} — a removed Profile is not a candidate.
     */
    @Query(value = """
            select p.member_id from profile p
            where p.removed_at is null
              and p.name_tsv @@ to_tsquery('simple'::regconfig, cast(:tsquery as text))
            order by ts_rank(p.name_tsv, to_tsquery('simple'::regconfig, cast(:tsquery as text))) desc,
                     p.member_id asc
            limit :limit
            """, nativeQuery = true)
    List<Long> nameCandidates(@Param("tsquery") String tsquery, @Param("limit") int limit);
}
