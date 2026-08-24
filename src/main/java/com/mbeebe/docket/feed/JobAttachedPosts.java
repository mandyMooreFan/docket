package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * §5.2.2: the job-attached Post — the ONLY path from board to feed, and it is
 * a member writing a Post, never syndication. The jobs module calls this from
 * the posting page's share form; the Post then rides every feed rule unchanged
 * (distribution, the Dial, §9.4's authored-as-minor fact, the read position).
 */
@Service
public class JobAttachedPosts {

    /** A refusal with an honest, member-facing reason — the caller's 422. */
    public static class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    private final PostRepository posts;
    private final JobBoardLookup board;
    private final Clock clock;

    JobAttachedPosts(PostRepository posts, JobBoardLookup board, Clock clock) {
        this.posts = posts;
        this.board = board;
        this.clock = clock;
    }

    /**
     * The member's words with one open posting attached. The words are the
     * Post — attaching is not reposting, so an empty body is refused like any
     * other Post's (§5.2.1's rule, unchanged).
     */
    @Transactional
    public long compose(Member author, long postingId, String rawBody) {
        String body = rawBody == null ? "" : rawBody.strip();
        if (body.isEmpty()) {
            throw new Refused("A post needs words — the posting card isn't a post.");
        }
        if (body.length() > PostService.MAX_BODY) {
            throw new Refused("A post can hold at most 40,000 characters.");
        }
        // §5.2.2: only one of the board's OPEN postings can be attached.
        boolean open = board.attached(postingId)
                .map(JobBoardLookup.AttachedPosting::open)
                .orElse(false);
        if (!open) {
            throw new Refused("That posting isn't open any more.");
        }
        // §9.4: the authored-as-minor fact, fixed here like everywhere else.
        return posts.save(Post.jobAttached(author.id(), body, postingId,
                author.isMinor(), clock.instant())).id();
    }
}
