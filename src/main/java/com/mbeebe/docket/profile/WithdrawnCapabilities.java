package com.mbeebe.docket.profile;

/**
 * What §10.3's ladder has taken from a Member, asked at the point of the gate.
 *
 * <p>The inverted seam the product uses everywhere for a cross-module read (cf. {@link
 * ConnectionLookup}): declared by the module that needs the answer, implemented by the
 * module that owns the facts. Moderation implements it over its own action rows; if
 * moderation is absent, nothing is withdrawn and every gate behaves exactly as it did
 * before the ladder existed.
 *
 * <p>Deliberately one question, not three. A Withdrawal takes one Capability, a
 * Suspension reduces a Member to reading only, and a Termination ends them — but every
 * §3.2 gate wants the same answer out of all three, and asking it here keeps the rungs
 * from leaking into eight call sites (ADR-0002: the facts are stored, this conclusion
 * is derived).
 */
public interface WithdrawnCapabilities {

    /** Whether this Member is, right now, refused this Capability by a moderation action. */
    boolean withdrawn(long memberId, Capability capability);
}
