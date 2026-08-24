package com.mbeebe.docket.graph;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface RecommendationRepository extends Repository<Recommendation, Long> {

    Recommendation save(Recommendation recommendation);

    Optional<Recommendation> findByAuthorIdAndSubjectId(Long authorId, Long subjectId);

    List<Recommendation> findBySubjectIdOrderByWrittenAt(Long subjectId);

    /**
     * The ones this Member wrote. §11.1 exports them (they provided them, so they
     * are portable); §11.2 leaves every one of them exactly where it is.
     */
    List<Recommendation> findByAuthorIdOrderByWrittenAt(Long authorId);
}
