package com.mbeebe.docket.company;

import java.util.List;

/**
 * What the company module asks of Positions, which the profile module owns. A
 * Position's currency gates the people list, page editing and job posting (§16),
 * derived at read time (ADR-0002); a merge repoints Positions wholesale (§10.5).
 * Implemented in com.mbeebe.docket.profile — the same seam shape as
 * {@code ConnectionLookup}.
 */
public interface CurrentPositions {

    /** Does this Member hold a current Position at this Company right now? */
    boolean heldBy(long memberId, long companyId);

    /** Every Member with a current Position at this Company (§8.5's people source). */
    List<Long> membersAt(long companyId);

    /**
     * Every Company where this Member holds a current Position — the candidate
     * set the jobs board (#35) intersects with the trust gate, and the far side
     * of "roles where I know someone" (§6.5).
     */
    List<Long> companiesHeldBy(long memberId);

    /**
     * Move every Position — current and past — from one Company to another, for a
     * merge. Returns the moved Position ids so the merge can record them (§10.5:
     * audited, reversible).
     */
    List<Long> repointAll(long fromCompanyId, long toCompanyId);
}
