package com.mbeebe.docket.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** What other modules may do with Companies; the repository stays internal. */
@Service
public class Companies {

    private final CompanyRepository repository;
    private final Clock clock;

    Companies(CompanyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * §6.1's creation rule: naming an employer reuses the existing Company —
     * case-insensitively, keeping its original casing — or creates it on the spot.
     */
    @Transactional
    public Company named(String name) {
        String stripped = name.strip();
        return repository.findByNameIgnoringCase(stripped)
                .orElseGet(() -> repository.save(new Company(stripped, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<String> namesStartingWith(String prefix, int limit) {
        return repository.findByNamePrefix(prefix.strip()).stream()
                .map(Company::name)
                .limit(limit)
                .toList();
    }
}
