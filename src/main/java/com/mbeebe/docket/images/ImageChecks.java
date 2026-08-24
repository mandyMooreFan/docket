package com.mbeebe.docket.images;

/**
 * §10.4's automation, all of it: the two hash checks run on every image upload,
 * before storage — CSAM hash-matching against a third-party service (the accepted
 * dent in cloud-agnosticism, §14.2) and the local blocklist of hashes taken down
 * under s.20A (§10.5). Neither is a judgement about speech; a refusal carries no
 * finding about the uploader. Every upload path in the product — company logos now,
 * feed and messaging images later (#33, #36) — MUST pass bytes through this port
 * before any row is created.
 */
public interface ImageChecks {

    /** True when the image may be stored; false means it never touches the database. */
    boolean permits(byte[] image);
}
