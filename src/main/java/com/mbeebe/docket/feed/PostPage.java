package com.mbeebe.docket.feed;

import java.util.List;

/**
 * The Post's own page: the Post as this viewer may see it, the Replies this
 * viewer may see (already filtered — the count and the list always agree,
 * §9.4), and the affordances this viewer actually holds.
 */
public record PostPage(PostView post, List<ReplyView> replies, boolean mayReply,
                       boolean closed, boolean mayModerate, boolean connectionsOnlyNote,
                       boolean viewerSignedIn) {
}
