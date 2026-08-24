package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface MemberTerminationRepository extends Repository<MemberTermination, Long> {

    MemberTermination save(MemberTermination termination);

    Optional<MemberTermination> findByMemberId(long memberId);

    boolean existsByMemberId(long memberId);
}
