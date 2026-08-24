package com.mbeebe.docket.company;

import org.springframework.data.repository.Repository;

interface CompanyMergeItemRepository extends Repository<CompanyMergeItem, Long> {

    CompanyMergeItem save(CompanyMergeItem item);
}
