package com.mbeebe.docket.feed;

import com.mbeebe.docket.leaving.Departure;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The feed at Termination (§11.2), and the clearest case of the line that section
 * draws: <em>anything that stood alone is unpublished.</em>
 *
 * <p><strong>Posts go.</strong> A Post stood alone — it was published on its own
 * account, into feeds, onto a Profile, into search. Deleting the row takes its
 * images and its V9 {@code body_tsv} entry with it, so it leaves feeds and search
 * in the same instant, with no reindex to remember.
 *
 * <p><strong>Replies stay</strong>, attributed to a former member, and this is a
 * decision rather than an omission. A Reply never stood alone: it sits inside
 * somebody else's thread, under somebody else's Post, where §11.2's whole argument
 * applies — removing it turns another member's conversation into a monologue of
 * holes, and that conversation is not the leaver's to edit. It is the same shape
 * as the Recommendation they wrote and the Thread they were half of, and it gets
 * the same answer.
 *
 * <p>Accepted cost, stated plainly: replies under other people's posts are a place
 * a departed member's words remain visible, and §11.2's "you cannot fully
 * disappear from Docket" covers this as much as it covers Threads.
 *
 * <p><strong>§9.4 does not move.</strong> The authored-as-minor fact lives on the
 * Reply row, not on the Member, precisely because §9.3 deletes the birth data the
 * derivation would need — so a minor's Reply is still omitted from every
 * logged-out view after its author has gone. Leaving cannot make anything more
 * visible than it was, which is the Children's code's floor, and the mechanism
 * that guarantees it is the one already in the schema.
 *
 * <p>Saves and the read mark go: private to the member, meaningful to nobody else.
 * A Save on somebody else's Post is not a mark on that Post — §5.3 says it is
 * counted nowhere — so removing it takes nothing from anyone.
 */
@Component
@Order(40)
class FeedDeparture implements Departure {

    private final PostRepository posts;
    private final SavedPostRepository saves;
    private final FeedVisitRepository visits;

    FeedDeparture(PostRepository posts, SavedPostRepository saves,
                  FeedVisitRepository visits) {
        this.posts = posts;
        this.saves = saves;
        this.visits = visits;
    }

    @Override
    @Transactional
    public void memberLeaving(long memberId) {
        saves.deleteAll(saves.findByMemberIdOrderBySavedAtDescIdDesc(memberId));
        visits.findById(memberId).ifPresent(visits::delete);
        // Last, and by row rather than in bulk: a Post's images, and the Replies
        // other members left on it, cascade from these rows (V6).
        posts.deleteAll(posts.findByAuthorIdOrderByCreatedAtDescIdDesc(memberId));
    }
}
