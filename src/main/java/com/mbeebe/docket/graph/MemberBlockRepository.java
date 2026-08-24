package com.mbeebe.docket.graph;

import org.springframework.data.repository.Repository;

import java.util.List;

interface MemberBlockRepository extends Repository<MemberBlock, Long> {

    MemberBlock save(MemberBlock block);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** §11.1: the Blocks you raised. Never the ones raised against you (§7.3). */
    List<MemberBlock> findByBlockerId(long blockerId);

    List<MemberBlock> findByBlockedId(long blockedId);

    void delete(MemberBlock block);
}
