package com.mbeebe.docket.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The page a Member publishes about themselves (CONTEXT.md) — exactly one per Member,
 * existing from the moment they join. Stores facts only; whether it is complete, who
 * may see it and whether it is indexable are derived on every read (ADR-0002).
 */
@Entity
@Table(name = "profile")
public class Profile {

    /** The single Dial (§4.1): who may see the whole Profile. No per-section matrix. */
    public enum Dial { PUBLIC, MEMBERS_ONLY, CONNECTIONS_ONLY }

    /**
     * Who the quiet open-to-work flag is shown to (§4.1). There is deliberately no
     * PUBLIC audience: never rendering the flag logged-out is what makes "never
     * searchable, never indexed" (§8.1) structural rather than a rule to remember.
     */
    public enum OpenToWork { OFF, CONNECTIONS, MEMBERS }

    @Id
    @Column(name = "member_id")
    private Long memberId;

    private String name;

    private String headline;

    private String location;

    private String summary;

    @Enumerated(EnumType.STRING)
    private Dial dial;

    @Enumerated(EnumType.STRING)
    @Column(name = "open_to_work")
    private OpenToWork openToWork;

    /**
     * The §4.1 photo: a pointer into the one image store (§10.4), null for the many
     * Profiles that will never have one. Never part of Completeness (§3.2) — nothing
     * derived from this column decides what its owner may do.
     */
    private Long photoImageId;

    /**
     * §10.3 rung 1, in {@code reply.removed_at}'s shape. A removed Profile stops
     * rendering on every surface, including §6.3's consented application view; the
     * row stays, because removal is never a delete and the Member is untouched —
     * ending a Member is the ladder's fourth rung, not this one.
     */
    @Column(name = "removed_at")
    private Instant removedAt;

    protected Profile() {
    }

    static Profile blankFor(long memberId) {
        Profile profile = new Profile();
        profile.memberId = memberId;
        profile.name = "";
        profile.headline = "";
        profile.location = "";
        profile.summary = "";
        profile.dial = Dial.PUBLIC;
        profile.openToWork = OpenToWork.OFF;
        return profile;
    }

    public Long memberId() {
        return memberId;
    }

    public String name() {
        return name;
    }

    public String headline() {
        return headline;
    }

    public String location() {
        return location;
    }

    public String summary() {
        return summary;
    }

    public Dial dial() {
        return dial;
    }

    public OpenToWork openToWork() {
        return openToWork;
    }

    public Long photoImageId() {
        return photoImageId;
    }

    void editBasics(String name, String headline, String location, String summary) {
        this.name = name.strip();
        this.headline = headline.strip();
        this.location = location.strip();
        this.summary = summary.strip();
    }

    void setDial(Dial dial) {
        this.dial = dial;
    }

    void setOpenToWork(OpenToWork openToWork) {
        this.openToWork = openToWork;
    }

    /** Setting and clearing are the same move — null is "back to initials". */
    void setPhoto(Long imageId) {
        this.photoImageId = imageId;
    }

    boolean removed() {
        return removedAt != null;
    }

    /** §10.3 rung 1: idempotent — the first removal is the one that stands. */
    void remove(Instant now) {
        if (removedAt == null) {
            removedAt = now;
        }
    }

    /**
     * §10.5: the dated fact lifted, the Profile back exactly as it was. Idempotent
     * — a Profile that is not removed is left alone.
     */
    void restore() {
        if (removedAt != null) {
            removedAt = null;
        }
    }
}
