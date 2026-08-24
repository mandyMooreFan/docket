package com.mbeebe.docket.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByPosition(long postId);
}
