package com.mbeebe.docket.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    Optional<SavedPost> findByMemberIdAndPostId(long memberId, long postId);

    boolean existsByMemberIdAndPostId(long memberId, long postId);

    List<SavedPost> findByMemberIdOrderBySavedAtDescIdDesc(long memberId);
}
