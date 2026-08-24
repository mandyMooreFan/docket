package com.mbeebe.docket.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

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
     * Naming an absorbed Company resolves to its survivor: the old name still
     * reaches the one entity it now is (§6.1's auto-merge, §10.5).
     */
    @Transactional
    public Company named(String name) {
        String stripped = name.strip();
        return repository.findByNameIgnoringCase(stripped)
                .map(this::resolved)
                .orElseGet(() -> repository.save(new Company(stripped, clock.instant())));
    }

    /** The Company as stored, absorbed or not — merge handling is the caller's. */
    @Transactional(readOnly = true)
    public Optional<Company> find(long id) {
        return repository.findById(id);
    }

    /** The Company this id means today: an absorbed id follows its merge chain. */
    @Transactional(readOnly = true)
    public Optional<Company> findResolved(long id) {
        return repository.findById(id).map(this::resolved);
    }

    Company resolved(Company company) {
        Company current = company;
        while (current.merged()) {
            current = repository.findById(current.mergedIntoId()).orElseThrow();
        }
        return current;
    }

    @Transactional(readOnly = true)
    public List<String> namesStartingWith(String prefix, int limit) {
        return repository.findByNamePrefix(prefix.strip()).stream()
                .map(Company::name)
                .limit(limit)
                .toList();
    }
}
