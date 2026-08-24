package com.mbeebe.docket.feed;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * What the feed template gets (§5.1, §13.4): the quiet replies-to-you section
 * (§5.5 — a section, never a badge or a count), everything since the last
 * visit, and the rail's real data. No numbers anywhere except each entry's
 * per-viewer reply count (§5.3, §5.6).
 */
public record FeedPage(boolean hasConnections, boolean mayPost,
                       List<ReplyNotice> repliesToYou, List<PostView> entries,
                       List<PersonCard> pendingRequests) {

    /** §5.5: one reply that reached you — to your Post or a thread you joined. */
    public record ReplyNotice(long postId, PersonCard author, String when, String excerpt) {
    }
}
