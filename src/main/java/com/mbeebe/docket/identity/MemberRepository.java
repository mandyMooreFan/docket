package com.mbeebe.docket.identity;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface MemberRepository extends Repository<Member, Long> {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    List<Member> findByAgeKind(Member.AgeKind ageKind);
}
