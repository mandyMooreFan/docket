package com.mbeebe.docket.identity;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface MemberRepository extends Repository<Member, Long> {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    /**
     * For {@link Addresses} only, and deliberately laxer than the login lookup: an
     * Invite must not mail an existing Member merely because the sender typed their
     * address in a different case (§13.3, §8.3).
     */
    Optional<Member> findByEmailIgnoreCase(String email);

    List<Member> findByAgeKind(Member.AgeKind ageKind);
}
