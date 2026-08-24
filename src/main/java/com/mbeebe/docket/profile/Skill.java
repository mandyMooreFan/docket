package com.mbeebe.docket.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A self-declared word on a Profile (CONTEXT.md). Nobody may attest to it. */
@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private String name;

    private Instant createdAt;

    protected Skill() {
    }

    Skill(long memberId, String name, Instant createdAt) {
        this.memberId = memberId;
        this.name = name;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    String name() {
        return name;
    }
}
