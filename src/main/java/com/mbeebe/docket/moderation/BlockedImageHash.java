package com.mbeebe.docket.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One image taken down under s.20A, remembered by its perceptual hash (§10.4.2).
 *
 * <p>§10.4 records that this is "more than the law requires", and says why it is worth
 * it: the person depicted reports once, not every time. The hash is not the image and
 * cannot be turned back into it — what is kept is a description of the picture's
 * gradients, which is enough to recognise a re-upload and not enough to reconstruct
 * anything.
 */
@Entity
@Table(name = "blocked_image_hash")
class BlockedImageHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hash;

    private Long reportId;

    private Instant addedAt;

    protected BlockedImageHash() {
    }

    BlockedImageHash(long hash, Long reportId, Instant now) {
        this.hash = hash;
        this.reportId = reportId;
        this.addedAt = now;
    }

    long hash() {
        return hash;
    }
}
