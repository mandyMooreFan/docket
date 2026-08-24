package com.mbeebe.docket.profile;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface EducationRepository extends Repository<EducationEntry, Long> {

    EducationEntry save(EducationEntry entry);

    Optional<EducationEntry> findByIdAndMemberId(Long id, Long memberId);

    List<EducationEntry> findByMemberIdOrderByCreatedAt(Long memberId);

    long countByMemberId(Long memberId);

    void delete(EducationEntry entry);
}
