package com.mbeebe.docket.profile;

/**
 * The answer to "may this Member do this?", with §10.3's distinction built into the
 * type: a capability never earned and one withdrawn are different states, and the
 * Member is told which they are in. WITHDRAWN joins when moderation lands (#38) —
 * it is declared now so every caller already handles the three-way answer.
 */
public enum CapabilityAnswer {

    YES,

    /** The Profile has not reached the §3.2 bar; finishing it is the remedy. */
    NOT_YET_EARNED,

    /** Removed by a moderation action (§10.3). Unreachable until #38 stores them. */
    WITHDRAWN
}
