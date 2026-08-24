package com.mbeebe.docket.invites;

import java.util.List;

/**
 * The Invite page as its sender may see it (§13.3), fully loaded (§14.2).
 *
 * <p>A sent Invite shows its address and its date, and <strong>nothing about what
 * became of it</strong>. That absence is the design, not an omission. "Joined"
 * would be §8.3's membership oracle told slowly, and "joined, but no request is
 * waiting" would hand the sender §9.2's age fact about a specific child. If the
 * person joins and accepts, they turn up in Connections like anyone else, which
 * is the only place that news belongs.
 */
public record InvitePage(boolean mayInvite, boolean justSent, String error,
                         List<Sent> sent) {

    /** One Invite this member sent: where it went and when. That is the whole row. */
    public record Sent(String email, String when) {
    }
}
