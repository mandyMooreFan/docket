package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row a merge repointed, recorded so reversal is mechanical rather than
 * forensic (§10.5): every moved Position, Work verification and edit-history row
 * is named here, and the absorbed Company's own row survives with a pointer.
 */
@Entity
@Table(name = "company_merge_item")
class CompanyMergeItem {

    enum Kind { POSITION, WORK_VERIFICATION, COMPANY_EDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long mergeId;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    private Long rowId;

    protected CompanyMergeItem() {
    }

    CompanyMergeItem(long mergeId, Kind kind, long rowId) {
        this.mergeId = mergeId;
        this.kind = kind;
        this.rowId = rowId;
    }
}
