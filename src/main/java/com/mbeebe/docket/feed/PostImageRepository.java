package com.mbeebe.docket.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByPosition(long postId);

    /** The reverse lookup /images/{id} needs to find out whose Post an image is on. */
    Optional<PostImage> findFirstByImageId(long imageId);
}
