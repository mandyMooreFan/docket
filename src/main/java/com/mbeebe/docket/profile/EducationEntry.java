package com.mbeebe.docket.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** An education entry — self-declared, and the §3.2 bar's alternative to a Position. */
@Entity
@Table(name = "education_entry")
public class EducationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private String institution;

    private String course;

    private Integer startYear;

    private Integer endYear;

    private Instant createdAt;

    protected EducationEntry() {
    }

    EducationEntry(long memberId, String institution, String course, Integer startYear,
                   Integer endYear, Instant createdAt) {
        this.memberId = memberId;
        this.institution = institution;
        this.course = course;
        this.startYear = startYear;
        this.endYear = endYear;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    String institution() {
        return institution;
    }

    String course() {
        return course;
    }

    /** "2019 — 2023", "2019 —", "— 2023" or empty: the years are optional. */
    String years() {
        if (startYear == null && endYear == null) {
            return "";
        }
        return (startYear == null ? "" : startYear) + " — " + (endYear == null ? "" : endYear);
    }
}
