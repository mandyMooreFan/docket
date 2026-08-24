package com.mbeebe.docket.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
