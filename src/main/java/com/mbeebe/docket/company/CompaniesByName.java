package com.mbeebe.docket.company;

import com.mbeebe.docket.search.CompanySearch;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The company module's answer to {@link CompanySearch} (§8.4: companies answer
 * logged out). Absorbed entities are absent — the same rule
 * {@link CompanyRepository#findByNamePrefix} keeps for autocomplete, so search
 * and autocomplete can never disagree about which Companies exist.
 */
@Component
class CompaniesByName implements CompanySearch {

    private final CompanyRepository repository;

    CompaniesByName(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hit> matching(String tsquery, int limit) {
        return repository.nameMatches(tsquery, limit).stream()
                .map(company -> new Hit(company.id(), company.name(), company.description()))
                .toList();
    }
}
