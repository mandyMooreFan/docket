package com.mbeebe.docket.identity;

import org.springframework.stereotype.Service;

import java.util.Optional;

/** What other modules may ask identity about a Member; the repository stays internal. */
@Service
public class Members {

    private final MemberRepository repository;

    Members(MemberRepository repository) {
        this.repository = repository;
    }

    public Optional<Member> find(long id) {
        return repository.findById(id);
    }
}
