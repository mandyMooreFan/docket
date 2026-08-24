package com.mbeebe.docket.feed;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * What a template gets of a Post — fully loaded, never the entity (§14.2).
 * {@code replyCount} is the only visible number in the feed (§5.3), derived for
 * this viewer at render time: it counts exactly the Replies this view shows.
 */
public record PostView(long id, PersonCard author, String when, String bodyHtml,
                       List<Long> imageIds, List<LinkPreview> links, int replyCount,
                       boolean saved, boolean mine,
                       JobBoardLookup.AttachedPosting attached) {

    public String replyCountLabel() {
        return replyCount == 1 ? "1 reply" : replyCount + " replies";
    }
}
