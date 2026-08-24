package com.mbeebe.docket.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A still image on a written Post (§5.2.1) — a reference into the one §10.4
 * image store, created only after that store's checks passed.
 */
@Entity
@Table(name = "post_image")
class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;

    private Long imageId;

    @Column(name = "position")
    private int position;

    protected PostImage() {
    }

    PostImage(long postId, long imageId, int position) {
        this.postId = postId;
        this.imageId = imageId;
        this.position = position;
    }

    long postId() {
        return postId;
    }

    long imageId() {
        return imageId;
    }
}
