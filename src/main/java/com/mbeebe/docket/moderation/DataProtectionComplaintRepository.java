package com.mbeebe.docket.moderation;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface DataProtectionComplaintRepository extends Repository<DataProtectionComplaint, Long> {

    DataProtectionComplaint save(DataProtectionComplaint complaint);

    Optional<DataProtectionComplaint> findById(long id);

    List<DataProtectionComplaint> findByRespondedAtIsNullOrderByCreatedAtAscIdAsc();
}
