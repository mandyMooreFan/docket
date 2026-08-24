package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.util.List;

interface BlockedImageHashRepository extends Repository<BlockedImageHash, Long> {

    BlockedImageHash save(BlockedImageHash hash);

    /**
     * Every blocked hash. Loaded whole because matching is a Hamming distance, not an
     * equality — no index answers "within ten bits of this". The list is the images
     * taken down under s.20A on one small instance, so scanning it costs less than the
     * arithmetic that produced the hash being compared.
     */
    List<BlockedImageHash> findAll();

    boolean existsByHash(long hash);
}
