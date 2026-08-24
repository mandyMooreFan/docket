package com.mbeebe.docket.company;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CompanyRepository extends Repository<Company, Long> {

    Company save(Company company);

    @Query("select c from Company c where lower(c.name) = lower(:name)")
    Optional<Company> findByNameIgnoringCase(@Param("name") String name);

    @Query("select c from Company c where lower(c.name) like lower(concat(:prefix, '%')) order by c.name")
    List<Company> findByNamePrefix(@Param("prefix") String prefix);
}
