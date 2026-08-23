package com.mbeebe.docket.skeleton;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "walking_skeleton")
class WalkingSkeleton {

    @Id
    private Long id;

    private String note;

    protected WalkingSkeleton() {
    }

    String note() {
        return note;
    }
}
