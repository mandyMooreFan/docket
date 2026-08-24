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

    /**
     * Sending an Invite (§13.3). Not named in §3.2's list because §13.3 wrote the
     * Invite later, but it belongs there on §3.2's own reasoning: an Invite is a
     * Connection request posted ahead of time to a stranger's inbox, and gating
     * the request while leaving the Invite open would be a hole straight through
     * the bar. The bar is "a real per-account cost for a bulk registrar", and
     * mailing strangers is precisely what a bulk registrar registers for.
     *
     * <p>This gates the SENDER only. §13.3's "optional, never a gate" is about the
     * invitee, whose signup stays completely open (§3.1) whether or not anyone
     * ever invited them.
     */
    INVITE(true),

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
