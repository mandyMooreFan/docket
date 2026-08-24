package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One accepted change to a Company page (§6.1): who, when, from what, to what.
 * Vandalism by a verified employee is a real, accepted surface, and these rows are
 * what makes it answerable (§10.5). For a logo the values are image ids.
 */
@Entity
@Table(name = "company_edit")
class CompanyEdit {

    enum Field { NAME, LOGO, DESCRIPTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private Field field;

    private String oldValue;

    private String newValue;

    private Instant editedAt;

    protected CompanyEdit() {
    }

    CompanyEdit(long companyId, long memberId, Field field, String oldValue, String newValue,
                Instant editedAt) {
        this.companyId = companyId;
        this.memberId = memberId;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.editedAt = editedAt;
    }

    Long id() {
        return id;
    }

    Long memberId() {
        return memberId;
    }

    Field field() {
        return field;
    }

    String oldValue() {
        return oldValue;
    }

    String newValue() {
        return newValue;
    }

    Instant editedAt() {
        return editedAt;
    }

    /** A merge folds the absorbed page's history into the survivor's (§6.1). */
    void repointTo(long companyId) {
        this.companyId = companyId;
    }
}
