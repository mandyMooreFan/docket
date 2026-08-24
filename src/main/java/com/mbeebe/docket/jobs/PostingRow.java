package com.mbeebe.docket.jobs;

/**
 * One board list row (§6.5) — fully loaded, never the entity (§14.2). The
 * salary is in every row (§6.3); there is deliberately no applicant count, no
 * badge, no score — nothing here is about the viewer.
 */
public record PostingRow(long id, String title, long companyId, String company,
                         String salaryLine, String placeLine, String postedOn) {
}
