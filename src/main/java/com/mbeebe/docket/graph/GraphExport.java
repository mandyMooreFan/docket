package com.mbeebe.docket.graph;

import com.mbeebe.docket.leaving.ExportContributor;
import com.mbeebe.docket.leaving.ExportDates;
import com.mbeebe.docket.leaving.ExportField;
import com.mbeebe.docket.leaving.ExportRecord;
import com.mbeebe.docket.leaving.ExportSection;
import com.mbeebe.docket.profile.PersonCard;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.stream.Stream;

/**
 * The graph's slice of the archive (§11.1): Connections, requests, Blocks, and
 * Recommendations in <strong>both</strong> directions.
 *
 * <p>Both directions is the §11.1 decision made concrete. A Recommendation you
 * wrote is yours under Article 20 — you provided it. A Recommendation somebody
 * wrote <em>about</em> you is not: you did not provide it, so portability does not
 * reach it. It is squarely your personal data all the same, so Article 15 does
 * ({@code docs/data-rights.md} §2). §11.1's answer is that members do not know the
 * difference and should not have to, so one button brings back both and the
 * archive labels which is which rather than making the member work it out.
 *
 * <p>Un-approved and hidden Recommendations are here too, with their state. They
 * are stored data about the member; §4.3 governs whether they <em>display</em>, and
 * that is a different question from whether they exist.
 *
 * <p>Blocks are one-sided on purpose: the Blocks this member raised, never the
 * Blocks raised against them. §4.2's silent-decline discipline and §7.3's
 * undetectable Block are the same rule, and an export that told you who had
 * blocked you would be the one door through which it leaked.
 */
@Component
@Order(30)
class GraphExport implements ExportContributor {

    private final ConnectionRepository connections;
    private final ConnectionRequestRepository requests;
    private final MemberBlockRepository blocks;
    private final RecommendationRepository recommendations;
    private final ProfileService profiles;
    private final Clock clock;

    GraphExport(ConnectionRepository connections, ConnectionRequestRepository requests,
                MemberBlockRepository blocks, RecommendationRepository recommendations,
                ProfileService profiles, Clock clock) {
        this.connections = connections;
        this.requests = requests;
        this.blocks = blocks;
        this.recommendations = recommendations;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        return List.of(connections(memberId), requests(memberId), blocks(memberId),
                written(memberId), received(memberId));
    }

    private ExportSection connections(long memberId) {
        return ExportSection.of("connections", "Your connections",
                connections.findByMemberAOrMemberBOrderByConnectedAtDesc(memberId, memberId)
                        .stream()
                        .map(connection -> {
                            PersonCard other = profiles.cardFor(connection.other(memberId));
                            return ExportRecord.of(other.displayName(), List.of(
                                    ExportField.of("member_id", "Member number",
                                            other.memberId()),
                                    ExportField.of("name", "Name", other.displayName()),
                                    ExportField.of("connected_on", "Connected on",
                                            ExportDates.on(connection.connectedAt(), clock))));
                        })
                        .toList());
    }

    /**
     * Requests in both directions, with what became of each — including the
     * declines you sent, which are yours. A decline you <em>received</em> is not in
     * here as a decline: §4.2 makes decline silent, and it stays silent in an
     * archive, so an outgoing request that was turned down reads exactly as one
     * still waiting. The row says otherwise; the export does not, and that is the
     * rule rather than an oversight.
     */
    private ExportSection requests(long memberId) {
        Stream<ExportRecord> sent = requests.findByRequesterId(memberId).stream()
                .map(request -> {
                    PersonCard other = profiles.cardFor(request.recipientId());
                    return ExportRecord.of("To " + other.displayName(), List.of(
                            ExportField.of("direction", "Direction", "You asked them"),
                            ExportField.of("member_id", "Member number", other.memberId()),
                            ExportField.of("name", "Name", other.displayName()),
                            ExportField.of("note", "Your note", request.note()),
                            ExportField.of("sent_on", "Sent on",
                                    ExportDates.on(request.sentAt(), clock)),
                            ExportField.of("state", "State", outgoing(request))));
                });
        Stream<ExportRecord> received = requests.findByRecipientId(memberId).stream()
                .map(request -> {
                    PersonCard other = profiles.cardFor(request.requesterId());
                    return ExportRecord.of("From " + other.displayName(), List.of(
                            ExportField.of("direction", "Direction", "They asked you"),
                            ExportField.of("member_id", "Member number", other.memberId()),
                            ExportField.of("name", "Name", other.displayName()),
                            ExportField.of("note", "Their note", request.note()),
                            ExportField.of("sent_on", "Sent on",
                                    ExportDates.on(request.sentAt(), clock)),
                            ExportField.of("state", "State", request.state().name())));
                });
        return ExportSection.of("connection_requests", "Connection requests",
                Stream.concat(sent, received).toList());
    }

    /** §4.2: a decline is silent, so an outgoing one never reads as declined. */
    private static String outgoing(ConnectionRequest request) {
        return request.state() == ConnectionRequest.State.ACCEPTED ? "ACCEPTED" : "SENT";
    }

    private ExportSection blocks(long memberId) {
        return ExportSection.of("blocks", "People you blocked",
                "Blocks you raised. Blocks raised against you are deliberately not "
                        + "here, and are not visible to you anywhere.",
                blocks.findByBlockerId(memberId).stream()
                        .map(block -> {
                            PersonCard other = profiles.cardFor(block.blockedId());
                            return ExportRecord.of(other.displayName(), List.of(
                                    ExportField.of("member_id", "Member number",
                                            other.memberId()),
                                    ExportField.of("name", "Name", other.displayName()),
                                    ExportField.of("blocked_on", "Blocked on",
                                            ExportDates.on(block.createdAt(), clock))));
                        })
                        .toList());
    }

    private ExportSection written(long memberId) {
        return ExportSection.of("recommendations_written", "Recommendations you wrote",
                recommendations.findByAuthorIdOrderByWrittenAt(memberId).stream()
                        .map(recommendation -> {
                            PersonCard about = profiles.cardFor(recommendation.subjectId());
                            return ExportRecord.of("About " + about.displayName(), List.of(
                                    ExportField.of("about_member_id", "About member number",
                                            about.memberId()),
                                    ExportField.of("about", "About", about.displayName()),
                                    ExportField.of("text", "What you wrote",
                                            recommendation.text()),
                                    ExportField.of("written_on", "Written on",
                                            ExportDates.on(recommendation.writtenAt(), clock)),
                                    ExportField.of("state", "State", state(recommendation))));
                        })
                        .toList());
    }

    /**
     * §11.1's Article 15 half: other people's words about you. Outside portability
     * — you did not provide them — and inside access, because they are about you.
     */
    private ExportSection received(long memberId) {
        return ExportSection.of("recommendations_received", "Recommendations about you",
                "Written by other people. They are here because they are about you.",
                recommendations.findBySubjectIdOrderByWrittenAt(memberId).stream()
                        .map(recommendation -> {
                            PersonCard author = profiles.cardFor(recommendation.authorId());
                            return ExportRecord.of("From " + author.displayName(), List.of(
                                    ExportField.of("author_member_id", "Author member number",
                                            author.memberId()),
                                    ExportField.of("author", "Author", author.displayName()),
                                    ExportField.of("text", "What they wrote",
                                            recommendation.text()),
                                    ExportField.of("written_on", "Written on",
                                            ExportDates.on(recommendation.writtenAt(), clock)),
                                    ExportField.of("state", "State", state(recommendation))));
                        })
                        .toList());
    }

    private static String state(Recommendation recommendation) {
        if (recommendation.hidden()) {
            return "Hidden";
        }
        return recommendation.displayed() ? "Showing" : "Waiting for approval";
    }
}
