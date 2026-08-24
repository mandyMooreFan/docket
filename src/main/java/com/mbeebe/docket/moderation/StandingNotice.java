package com.mbeebe.docket.moderation;

/**
 * One rung standing against a Member right now, in the words they read (§10.3).
 *
 * <p>Everything is a String or a primitive on purpose. This is the surface that has to
 * carry "a capability never earned and a capability withdrawn are different states, and
 * the member is told which they are in", so the template should be choosing words, not
 * reaching through a domain object to reach them.
 */
public record StandingNotice(long actionId,
                             String kind,
                             String capability,
                             String reason,
                             String until,
                             boolean indefinite) {

    public boolean withdrawal() {
        return "WITHDRAWAL".equals(kind);
    }

    public boolean suspension() {
        return "SUSPENSION".equals(kind);
    }

    public boolean termination() {
        return "TERMINATION".equals(kind);
    }
}
