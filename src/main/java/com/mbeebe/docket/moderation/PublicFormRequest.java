package com.mbeebe.docket.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One accepted submission of a public, no-account form, in identity's {@code
 * link_request} shape (§3.3, §8.3).
 *
 * <p>Write-only, deliberately: there are no getters, because counting happens in the
 * repository and nothing should be tempted to read these rows back. The important
 * property is that a row is written for every accepted request, before and
 * independently of whether anything real happens downstream — a limiter whose
 * behaviour changed depending on what the submission turned out to point at would
 * answer questions about the site to anyone patient enough to ask.
 */
@Entity
@Table(name = "public_form_request")
class PublicFormRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Form form;

    private String contact;

    private String requestIp;

    private Instant createdAt;

    /** The two routes §10.5 and §11.3 each insist on having to themselves. */
    enum Form {
        INTIMATE_IMAGE,
        DATA_PROTECTION
    }

    protected PublicFormRequest() {
    }

    PublicFormRequest(Form form, String contact, String requestIp, Instant now) {
        this.form = form;
        this.contact = contact;
        this.requestIp = requestIp;
        this.createdAt = now;
    }
}
