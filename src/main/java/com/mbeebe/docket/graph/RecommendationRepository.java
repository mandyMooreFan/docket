package com.mbeebe.docket.graph;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface RecommendationRepository extends Repository<Recommendation, Long> {

    Recommendation save(Recommendation recommendation);

    Optional<Recommendation> findByAuthorIdAndSubjectId(Long authorId, Long subjectId);

    List<Recommendation> findBySubjectIdOrderByWrittenAt(Long subjectId);
}
