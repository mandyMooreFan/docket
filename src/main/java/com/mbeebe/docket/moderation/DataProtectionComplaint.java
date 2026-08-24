package com.mbeebe.docket.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A data-protection complaint (DUAA s.164A, §11.3, §15.3) — its own route, because the
 * statute makes it one. Distinct from a Report and from the intimate-image route, and
 * the product says so on all three forms so nobody has to guess which door they are at.
 *
 * <p>The word "complaint" is used here and nowhere else. CONTEXT.md puts it on the
 * <em>Avoid</em> list for a Report precisely so that it stays available for this, which
 * is the thing the law actually calls a complaint.
 *
 * <p>{@code acknowledgedAt} is set on receipt, not on a thirty-day timer. The duty is
 * to acknowledge within 30 days; acknowledging immediately means there is no window in
 * which the duty is running, which is the same shape §10.5 uses for its 48 hours.
 */
@Entity
@Table(name = "data_protection_complaint")
class DataProtectionComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contact;

    private String account;

    private String requestIp;

    private Instant createdAt;

    private Instant acknowledgedAt;

    private Instant respondedAt;

    private String response = "";

    protected DataProtectionComplaint() {
    }

    DataProtectionComplaint(String contact, String account, String requestIp, Instant now) {
        this.contact = contact;
        this.account = account;
        this.requestIp = requestIp;
        this.createdAt = now;
        this.acknowledgedAt = now;
    }

    Long id() {
        return id;
    }

    String contact() {
        return contact;
    }

    String account() {
        return account;
    }

    Instant createdAt() {
        return createdAt;
    }

    boolean open() {
        return respondedAt == null;
    }

    void respond(String response, Instant now) {
        if (respondedAt == null) {
            this.response = response;
            this.respondedAt = now;
        }
    }
}
