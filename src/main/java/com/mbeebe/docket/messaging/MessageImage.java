package com.mbeebe.docket.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A still image on a Message (§7.2), in the feed's post_image shape — a
 * reference into the one §10.4 image store, written only after that store's
 * checks passed. Correspondence is private, so the bytes are never served from
 * the shared /images path: see MessagesController's thread-scoped route.
 */
@Entity
@Table(name = "message_image")
class MessageImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long messageId;

    private Long imageId;

    @Column(name = "position")
    private int position;

    protected MessageImage() {
    }

    MessageImage(long messageId, long imageId, int position) {
        this.messageId = messageId;
        this.imageId = imageId;
        this.position = position;
    }

    long imageId() {
        return imageId;
    }
}
