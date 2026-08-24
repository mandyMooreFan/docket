package com.mbeebe.docket.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthorIdOrderByCreatedAtDescIdDesc(long authorId);

    /**
     * The feed's whole query (§5.1): the mutual graph and nothing else, strictly
     * after the read position, newest first. Ids break timestamp ties so the
     * order is total and the high-water mark never lets a Post show twice.
     */
    List<Post> findByAuthorIdInAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            Collection<Long> authorIds, Instant after);
}
