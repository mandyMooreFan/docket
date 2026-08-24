package com.mbeebe.docket.company;

import org.springframework.data.repository.Repository;

import java.util.List;

interface CompanyEditRepository extends Repository<CompanyEdit, Long> {

    CompanyEdit save(CompanyEdit edit);

    List<CompanyEdit> findByCompanyIdOrderByIdAsc(long companyId);

    List<CompanyEdit> findByCompanyIdOrderByIdDesc(long companyId);
}
