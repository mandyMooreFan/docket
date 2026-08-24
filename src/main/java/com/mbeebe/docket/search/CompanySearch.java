package com.mbeebe.docket.search;

import java.util.List;

/**
 * What search asks the company module (§8.4: companies are searchable logged
 * out). Implemented in com.mbeebe.docket.company, which owns the one rule that
 * matters here: an absorbed Company is not a Company any more (§6.1, §10.5), so
 * it never appears in results even though its row survives for reversal.
 *
 * <p>There is no viewer parameter, and that is the point: a Company page is
 * public. Its PEOPLE list is account-gated (§8.4) and lives on the page, not
 * here — search never hands anyone a set of people defined by their employer.
 */
public interface CompanySearch {

    List<Hit> matching(String tsquery, int limit);

    record Hit(long id, String name, String description) {
    }
}
