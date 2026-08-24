package com.mbeebe.docket.graph;

import org.springframework.data.repository.Repository;

interface MemberBlockRepository extends Repository<MemberBlock, Long> {

    MemberBlock save(MemberBlock block);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
