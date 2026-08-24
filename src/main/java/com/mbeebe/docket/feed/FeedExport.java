package com.mbeebe.docket.feed;

import com.mbeebe.docket.leaving.ExportContributor;
import com.mbeebe.docket.leaving.ExportDates;
import com.mbeebe.docket.leaving.ExportField;
import com.mbeebe.docket.leaving.ExportMedia;
import com.mbeebe.docket.leaving.ExportRecord;
import com.mbeebe.docket.leaving.ExportSection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * The feed's slice of the archive (§11.1): everything this member wrote for the
 * feed, and the private Saves nobody else has ever seen.
 *
 * <p>Post text comes out as the member typed it, not as the page renders it —
 * {@code Prose.toHtml}'s escaping and linkifying is presentation, and an archive
 * that handed back {@code &amp;amp;} where a person typed {@code &} would be a copy
 * of the page rather than a copy of the data. Images come out as bytes.
 *
 * <p>The authored-as-minor fact (§9.4) is exported, and it is the one field here a
 * member might be surprised to find. It is stored, it is about them, and it is the
 * reason a piece of their writing behaves differently from another piece — so it
 * belongs in an access copy, labelled in words rather than as a boolean.
 *
 * <p>Reply <em>counts</em> and the feed's read position are absent. The count is
 * derived per viewer and never stored (ADR-0002); the read mark is a single
 * high-water instant that is about the mechanics of a page, not about a person.
 */
@Component
@Order(40)
class FeedExport implements ExportContributor {

    private final PostRepository posts;
    private final PostImageRepository postImages;
    private final ReplyRepository replies;
    private final SavedPostRepository saves;
    private final Clock clock;

    FeedExport(PostRepository posts, PostImageRepository postImages, ReplyRepository replies,
               SavedPostRepository saves, Clock clock) {
        this.posts = posts;
        this.postImages = postImages;
        this.replies = replies;
        this.saves = saves;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        return List.of(posts(memberId), replies(memberId), saves(memberId));
    }

    private ExportSection posts(long memberId) {
        return ExportSection.of("posts", "Your posts",
                posts.findByAuthorIdOrderByCreatedAtDescIdDesc(memberId).stream()
                        .map(post -> ExportRecord.of(
                                ExportDates.at(post.createdAt(), clock),
                                List.of(ExportField.of("written_at", "Written",
                                                ExportDates.at(post.createdAt(), clock)),
                                        ExportField.of("kind", "Kind", kind(post.kind())),
                                        ExportField.of("body", "What you wrote", post.body()),
                                        ExportField.of("thread", "Replies",
                                                post.threadClosed()
                                                        ? "You closed this thread"
                                                        : "Open"),
                                        ExportField.of("authored_as_minor",
                                                "Written before you turned 18",
                                                yesNo(post.authoredAsMinor()))),
                                postImages.findByPostIdOrderByPosition(post.id()).stream()
                                        .map(image -> ExportMedia.of(image.imageId(), "post"))
                                        .toList()))
                        .toList());
    }

    private ExportSection replies(long memberId) {
        return ExportSection.of("replies", "Your replies",
                "Replies you left under other people's posts. These stay on Docket if "
                        + "you delete your account, marked as written by a former member.",
                replies.findByAuthorIdOrderByCreatedAtDescIdDesc(memberId).stream()
                        .map(reply -> ExportRecord.of(
                                ExportDates.at(reply.createdAt(), clock),
                                List.of(ExportField.of("written_at", "Written",
                                                ExportDates.at(reply.createdAt(), clock)),
                                        ExportField.of("post_id", "On post number",
                                                reply.postId()),
                                        ExportField.of("body", "What you wrote", reply.body()),
                                        ExportField.of("authored_as_minor",
                                                "Written before you turned 18",
                                                yesNo(reply.authoredAsMinor())))))
                        .toList());
    }

    /** §5.3's private Save: visible to nobody but its owner, counted nowhere. */
    private ExportSection saves(long memberId) {
        return ExportSection.of("saved_posts", "Posts you saved",
                "Private to you. Nobody was ever told you saved these.",
                saves.findByMemberIdOrderBySavedAtDescIdDesc(memberId).stream()
                        .map(saved -> ExportRecord.of("", List.of(
                                ExportField.of("post_id", "Post number", saved.postId()),
                                ExportField.of("saved_on", "Saved on",
                                        ExportDates.on(saved.savedAt(), clock)))))
                        .toList());
    }

    private static String kind(Post.Kind kind) {
        return switch (kind) {
            case WRITTEN -> "Something you wrote";
            case WORK_CHANGE -> "A work change you shared";
            case JOB_ATTACHED -> "A post with a job attached";
        };
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}
