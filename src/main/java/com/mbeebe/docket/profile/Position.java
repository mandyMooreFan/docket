package com.mbeebe.docket.profile;

import com.mbeebe.docket.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A Member's self-declared claim to a role, current or past, optionally at a Company
 * (CONTEXT.md). Month resolution; a null end is what "current" means — currency is
 * derived on read, never flagged (ADR-0002), because #34 hangs capability off it.
 */
@Entity
@Table(name = "position")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    private String title;

    private String description;

    @Column(name = "start_month")
    private LocalDate startMonth;

    @Column(name = "end_month")
    private LocalDate endMonth;

    private Instant createdAt;

    protected Position() {
    }

    Position(long memberId, Company company, String title, String description,
             YearMonth start, Instant createdAt) {
        this.memberId = memberId;
        this.company = company;
        this.title = title;
        this.description = description;
        this.startMonth = start.atDay(1);
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    Long memberId() {
        return memberId;
    }

    Company company() {
        return company;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    YearMonth start() {
        return YearMonth.from(startMonth);
    }

    YearMonth end() {
        return endMonth == null ? null : YearMonth.from(endMonth);
    }

    boolean current() {
        return endMonth == null;
    }

    /** Ending is self-reported, like every claim on a Profile (§16). */
    void endAt(YearMonth end) {
        this.endMonth = end.atDay(1);
    }
}
