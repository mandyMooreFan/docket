package com.mbeebe.docket.images;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Stored image bytes — created only after the §10.4 hash checks passed. Rows are
 * immutable: a new image is a new row, which is what lets /images/{id} be cached
 * forever.
 */
@Entity
@Table(name = "image")
class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contentType;

    private byte[] data;

    private Instant createdAt;

    protected Image() {
    }

    Image(String contentType, byte[] data, Instant createdAt) {
        this.contentType = contentType;
        this.data = data;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    String contentType() {
        return contentType;
    }

    byte[] data() {
        return data;
    }
}
