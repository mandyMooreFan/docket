package com.mbeebe.docket.identity;

/**
 * What identity announces the moment a Member first exists (§3.1) — the seam the
 * Invite's waiting Connection request hangs on (§13.3).
 *
 * <p>Fired on creation only, from inside the transaction that created the Member,
 * so a listener that throws takes the whole join down rather than leaving half a
 * member behind. Never fired for a sign-in, and never fired for a refused
 * under-16, who does not become a Member at all — §3.1 stores nothing about that
 * refusal, and there is nothing here for an Invite to land on either.
 *
 * <p>Implemented outside identity. Identity deliberately learns nothing about
 * what a listener does with the news.
 */
public interface JoinListener {

    void joined(Member member);
}
