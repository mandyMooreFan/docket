package com.mbeebe.docket.profile;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface SkillRepository extends Repository<Skill, Long> {

    Skill save(Skill skill);

    Optional<Skill> findByIdAndMemberId(Long id, Long memberId);

    Optional<Skill> findByMemberIdAndNameIgnoringCase(Long memberId, String name);

    List<Skill> findByMemberIdOrderByCreatedAt(Long memberId);

    void delete(Skill skill);
}
