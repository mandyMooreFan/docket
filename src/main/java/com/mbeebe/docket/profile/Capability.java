package com.mbeebe.docket.profile;

/**
 * Something a Member may do (CONTEXT.md) — the §3.2 list withheld until Completeness.
 * Never held or granted: always a conclusion drawn at the point of asking (ADR-0002).
 * Consumption — browsing, reading the feed, the jobs board, editing your own Profile —
 * is deliberately not here; it is never gated.
 */
public enum Capability {

    /** Sending a Connection request (§4.2). */
    CONNECT,

    /** Writing a Message (§7). */
    MESSAGE,

    /** Writing a Post (§5). */
    POST,

    /**
     * Posting a Job (§6.3). Completeness is necessary but not sufficient: the trust
     * gate (#34) adds a current Position plus a Work verification at its domain,
     * composed where the jobs board asks.
     */
    POST_JOB
}
