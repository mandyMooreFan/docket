package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface AppealRepository extends Repository<Appeal, Long> {

    Appeal save(Appeal appeal);

    Optional<Appeal> findById(long id);

    Optional<Appeal> findByActionId(long actionId);

    boolean existsByActionId(long actionId);

    List<Appeal> findByDecidedAtIsNullOrderByMadeAtAscIdAsc();

    List<Appeal> findByMemberIdOrderByMadeAtDesc(long memberId);
}
