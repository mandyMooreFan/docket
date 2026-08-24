package com.mbeebe.docket.company;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface WorkLinkRepository extends Repository<WorkLink, Long> {

    WorkLink save(WorkLink link);

    Optional<WorkLink> findByTokenHash(String tokenHash);
}
