package com.mbeebe.docket.profile;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface ProfileRepository extends Repository<Profile, Long> {

    Profile save(Profile profile);

    Optional<Profile> findById(Long memberId);
}
