package com.mbeebe.docket.leaving;

import java.util.List;

/**
 * How an owning module hands over the rows it holds about one Member (§11.1).
 *
 * <p>The same inverted dependency as {@code images.ImageAudience}, for the same
 * reason: the export must be a copy of what is actually stored, and only the
 * module that stores it knows what that is. This module states the question; each
 * owning module answers for its own tables; nothing about Posts, Threads or
 * Applications leaks into this package. The alternative — one exporter reaching
 * into eight schemas — would be one class that has to be edited every time any
 * other module grows a column, and would quietly go stale the first time somebody
 * forgot.
 *
 * <p>A contributor returns <em>stored facts only</em>. Derived Capabilities and
 * effective visibility are never stored (ADR-0002) and fall outside Article 20 as
 * a category (WP242 p.10, {@code docs/data-rights.md} §2), so there is nothing
 * here for them to be exported from. That absence is stated in the archive's
 * supplementary information rather than left for the member to notice.
 *
 * <p>Contributors are asked in {@code @Order} sequence and their sections appear
 * in that order, so the archive reads the same way twice.
 */
public interface ExportContributor {

    /**
     * This module's sections for one Member, in reading order. Empty sections are
     * allowed and are kept: "you have no saved searches" is itself an answer, and
     * §13.4's honesty about empty applies inside an archive too.
     */
    List<ExportSection> sectionsFor(long memberId);
}
