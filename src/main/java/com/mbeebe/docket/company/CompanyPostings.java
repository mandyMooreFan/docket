package com.mbeebe.docket.company;

import java.util.List;

/**
 * What the Company page asks of the jobs board (§6.1: a Company is a name, a
 * logo, a description, its postings, its people): the open postings to list,
 * already shaped for rendering. Implemented in com.mbeebe.docket.jobs — the
 * same seam shape as {@link CurrentPositions}, pointing the other way.
 */
public interface CompanyPostings {

    /** This Company's open postings, newest first — derived from the window (§6.3). */
    List<Entry> openAt(long companyId);

    record Entry(long id, String title, String salaryLine, String placeLine) {
    }
}
