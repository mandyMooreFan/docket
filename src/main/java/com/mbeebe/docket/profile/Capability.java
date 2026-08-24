package com.mbeebe.docket.profile;

/**
 * Something a Member may do (CONTEXT.md) — the §3.2 list withheld until Completeness,
 * plus the one thing §10.3 can take away that Completeness never gave. Never held or
 * granted: always a conclusion drawn at the point of asking (ADR-0002). Consumption —
 * browsing, reading the feed, the jobs board, editing your own Profile — is
 * deliberately not here; it is never gated.
 */
public enum Capability {

    /** Sending a Connection request (§4.2). */
    CONNECT(true),

    /** Writing a Message (§7). */
    MESSAGE(true),

    /** Writing a Post (§5). */
    POST(true),

    /**
     * Posting a Job (§6.3). Completeness is necessary but not sufficient: the trust
     * gate (#34) adds a current Position plus a Work verification at its domain,
     * composed where the jobs board asks.
     */
    POST_JOB(true),

    /**
     * Writing a Reply (§5.3). The odd one: §3.2 never withheld it, because the
     * Connection was the earned thing — so it is only ever YES or WITHDRAWN, never
     * NOT_YET_EARNED. §10.3 names Replies among the four things a Withdrawal can take,
     * and proportionality is the point: someone abusing Replies does not thereby lose
     * their correspondence.
     */
    REPLY(false);

    private final boolean earnedByCompleteness;

    Capability(boolean earnedByCompleteness) {
        this.earnedByCompleteness = earnedByCompleteness;
    }

    /** Whether §3.2's bar stands between a Member and this, before moderation is asked. */
    boolean earnedByCompleteness() {
        return earnedByCompleteness;
    }
}
