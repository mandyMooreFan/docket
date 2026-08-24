package com.mbeebe.docket.company;

import org.springframework.data.repository.Repository;

import java.util.List;

interface CompanyEditRepository extends Repository<CompanyEdit, Long> {

    CompanyEdit save(CompanyEdit edit);

    List<CompanyEdit> findByCompanyIdOrderByIdAsc(long companyId);

    List<CompanyEdit> findByCompanyIdOrderByIdDesc(long companyId);

    /** §11.1: the edits one Member made, wherever they made them. */
    List<CompanyEdit> findByMemberIdOrderByIdAsc(long memberId);
}
