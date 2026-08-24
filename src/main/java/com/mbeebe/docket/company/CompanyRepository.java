package com.mbeebe.docket.company;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CompanyRepository extends Repository<Company, Long> {

    Company save(Company company);

    Optional<Company> findById(Long id);

    /**
     * Whether any standing Company currently wears this logo — the images module's
     * claim (§8.4). A removed Company (§10.3) stops claiming its logo, so the bytes
     * stop being served with it.
     */
    boolean existsByLogoImageIdAndRemovedAtIsNull(long imageId);

    @Query("select c from Company c where lower(c.name) = lower(:name)")
    Optional<Company> findByNameIgnoringCase(@Param("name") String name);

    /**
     * Autocomplete never advertises an absorbed Company's name (§10.5's tidy-up),
     * and never a removed one either (§10.3 rung 1) — both predicates live in the
     * query, so a new caller cannot forget either.
     */
    @Query("select c from Company c where c.mergedIntoId is null and c.removedAt is null "
            + "and lower(c.name) like lower(concat(:prefix, '%')) order by c.name")
    List<Company> findByNamePrefix(@Param("prefix") String prefix);

    /**
     * §8's company group: the name index, and the same rule autocomplete keeps
     * — an absorbed Company is never advertised (§6.1's auto-merge, §10.5). The
     * merge is read from merged_into_id at query time, not baked into the
     * index, because a merge is a reversible fact. A removed Company (§10.3) is
     * absent for the same reason and by the same means.
     *
     * <p>§8.2's order: ts_rank, then the id, so it is total and impersonal.
     */
    @Query(value = """
            select c.* from company c
            where c.merged_into_id is null and c.removed_at is null
              and c.name_tsv @@ to_tsquery('simple'::regconfig, cast(:tsquery as text))
            order by ts_rank(c.name_tsv, to_tsquery('simple'::regconfig, cast(:tsquery as text))) desc,
                     c.id asc
            limit :limit
            """, nativeQuery = true)
    List<Company> nameMatches(@Param("tsquery") String tsquery, @Param("limit") int limit);
}
