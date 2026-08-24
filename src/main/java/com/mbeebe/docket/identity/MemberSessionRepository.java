package com.mbeebe.docket.identity;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface MemberSessionRepository extends Repository<MemberSession, Long> {

    MemberSession save(MemberSession session);

    Optional<MemberSession> findByTokenHash(String tokenHash);

    List<MemberSession> findByMemberOrderByLastUsedAtDesc(Member member);

    void deleteByTokenHash(String tokenHash);

    void deleteByMember(Member member);
}
