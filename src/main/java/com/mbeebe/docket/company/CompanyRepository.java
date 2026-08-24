package com.mbeebe.docket.company;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CompanyRepository extends Repository<Company, Long> {

    Company save(Company company);

    Optional<Company> findById(Long id);

    /** Whether any Company currently wears this logo — the images module's claim (§8.4). */
    boolean existsByLogoImageId(long imageId);

    @Query("select c from Company c where lower(c.name) = lower(:name)")
    Optional<Company> findByNameIgnoringCase(@Param("name") String name);

    /** Autocomplete never advertises an absorbed Company's name (§10.5's tidy-up). */
    @Query("select c from Company c where c.mergedIntoId is null "
            + "and lower(c.name) like lower(concat(:prefix, '%')) order by c.name")
    List<Company> findByNamePrefix(@Param("prefix") String prefix);
}
