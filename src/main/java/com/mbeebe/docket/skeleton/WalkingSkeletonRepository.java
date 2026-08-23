package com.mbeebe.docket.skeleton;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface WalkingSkeletonRepository extends Repository<WalkingSkeleton, Long> {

    Optional<WalkingSkeleton> findById(Long id);
}
