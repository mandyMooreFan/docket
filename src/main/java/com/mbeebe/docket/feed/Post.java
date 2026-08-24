package com.mbeebe.docket.feed;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Post (SPEC.md §5.2, CONTEXT.md): something a Member wrote for the feed.
 * Written and work-change kinds exist today; job-attached arrives with the jobs
 * board (#35). {@code authoredAsMinor} is the §9.4 fact — fixed at creation,
 * with no mutator anywhere, because the birth data it derives from is deleted
 * at the 18 rollover (§9.3) and the cap it drives is permanent.
 */
@Entity
@Table(name = "post")
class Post {

    enum Kind { WRITTEN, WORK_CHANGE, JOB_ATTACHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long authorId;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    private String body;

    /** §9.4: immutable at creation; the 18 rollover never lifts this. */
    private boolean authoredAsMinor;

    private Instant threadClosedAt;

    private Instant createdAt;

    /** §5.2.2: set only on a JOB_ATTACHED Post — the board reference it carries. */
    private Long jobPostingId;

    protected Post() {
    }

    Post(long authorId, Kind kind, String body, boolean authoredAsMinor, Instant createdAt) {
        this.authorId = authorId;
        this.kind = kind;
        this.body = body;
        this.authoredAsMinor = authoredAsMinor;
        this.createdAt = createdAt;
    }

    /** §5.2.2: a member's words with one of the board's postings attached. */
    static Post jobAttached(long authorId, String body, long jobPostingId,
                            boolean authoredAsMinor, Instant createdAt) {
        Post post = new Post(authorId, Kind.JOB_ATTACHED, body, authoredAsMinor, createdAt);
        post.jobPostingId = jobPostingId;
        return post;
    }

    Long id() {
        return id;
    }

    long authorId() {
        return authorId;
    }

    Kind kind() {
        return kind;
    }

    String body() {
        return body;
    }

    boolean authoredAsMinor() {
        return authoredAsMinor;
    }

    boolean threadClosed() {
        return threadClosedAt != null;
    }

    /** §5.3: the author may close the thread — no further Replies, a dated fact. */
    void closeThread(Instant now) {
        if (threadClosedAt == null) {
            threadClosedAt = now;
        }
    }

    Instant createdAt() {
        return createdAt;
    }

    Long jobPostingId() {
        return jobPostingId;
    }
}
