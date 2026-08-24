package com.mbeebe.docket.identity;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface MagicLinkRepository extends Repository<MagicLink, Long> {

    MagicLink save(MagicLink link);

    Optional<MagicLink> findByTokenHash(String tokenHash);
}
