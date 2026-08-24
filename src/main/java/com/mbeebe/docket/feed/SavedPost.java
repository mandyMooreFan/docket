package com.mbeebe.docket.feed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A Save (§5.3, CONTEXT.md): a Member's private bookmark. Visible to nobody else. */
@Entity
@Table(name = "saved_post")
class SavedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long postId;

    private Instant savedAt;

    protected SavedPost() {
    }

    SavedPost(long memberId, long postId, Instant savedAt) {
        this.memberId = memberId;
        this.postId = postId;
        this.savedAt = savedAt;
    }

    long postId() {
        return postId;
    }

    Instant savedAt() {
        return savedAt;
    }
}
