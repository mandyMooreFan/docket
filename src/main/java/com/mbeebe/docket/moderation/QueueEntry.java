package com.mbeebe.docket.moderation;

import java.time.Instant;
import java.util.Optional;

/**
 * One open Report as the queue shows it, with the item itself alongside (§10.1).
 *
 * <p>A public view record for the same reason {@code FeedPage} is one: the service
 * layer hands templates fully-loaded view models, and a template cannot reflect into a
 * package-private type. Nothing here is an entity — the queue reads facts, it does not
 * hold rows open across a render.
 */
public record QueueEntry(long reportId,
                         TargetKind targetKind,
                         ReportCategory category,
                         String account,
                         Instant createdAt,
                         Optional<ReportableContent.ReportedItem> item) {

    /** An item that has since gone: the Report still needs deciding, honestly. */
    public boolean itemGone() {
        return item.isEmpty();
    }
}
