package com.mbeebe.docket.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An employer, existing because a Member named it while adding a Position (§6.1).
 * Never an actor: no account, no owner, nobody speaks for it. This increment holds
 * only the name; logo, description, pages and the trust gate arrive with #34.
 */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Instant createdAt;

    protected Company() {
    }

    Company(String name, Instant createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }
}
