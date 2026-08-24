package com.mbeebe.docket.company;

import org.springframework.data.repository.Repository;

interface CompanyMergeRepository extends Repository<CompanyMerge, Long> {

    CompanyMerge save(CompanyMerge merge);
}
