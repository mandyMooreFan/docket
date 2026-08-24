package com.mbeebe.docket.profile;

/**
 * §5.2.3's seam: how the Profile edit flows hand genuine news — started a
 * role, left a role — to the feed module (#33), only when the member ticked
 * the box at that moment. The same one-interface coupling as
 * {@link ProfileGraphLookup}. Never called without the tick: not calling this
 * is what "never automatic, never retroactive" compiles to.
 */
public interface WorkChangeAnnouncer {

    void roleStarted(long memberId, String title, String companyName);

    void roleEnded(long memberId, String title, String companyName);
}
