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
 * checks passed. Who may read the bytes back is answered by
 * {@link MessageImageAudience}, the messaging module's contribution to the one
 * guard in front of /images/{id}.
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

    long messageId() {
        return messageId;
    }

    long imageId() {
        return imageId;
    }
}
