package com.mbeebe.docket.search;

import com.mbeebe.docket.graph.Connections;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.PersonCard;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * One box, four groups (§8). This class composes; it decides nothing about who
 * may see what — every such answer comes back through a seam from the module
 * that owns the rule, so a Dial, a Block, a floor or a closed window is applied
 * once, in one place, by the code that already applies it everywhere else
 * (§8.5).
 *
 * <p>What is NOT here, and could not be added without showing up in the
 * diff: any input to any group that describes the asker. §8.2's rule is that the
 * same query returns the same results in the same order for everyone, so the
 * viewer reaches the groups for exactly two reasons — deciding what they are
 * allowed to see, and answering the box they ticked themselves. Mutuals are
 * attached after the order is fixed and cannot disturb it.
 */
@Service
class SearchService {

    /** A results page is a page, not a feed: ten to a group, the same ten for everyone. */
    static final int GROUP_LIMIT = 10;

    /** §8.2: the shared Connections shown on a row — a glance, not a list. */
    private static final int MUTUALS_SHOWN = 3;

    private final PeopleSearch people;
    private final CompanySearch companies;
    private final PostSearch posts;
    private final PostingSearch postings;
    private final Connections graph;
    private final ProfileService profiles;

    SearchService(PeopleSearch people, CompanySearch companies, PostSearch posts,
                  PostingSearch postings, Connections graph, ProfileService profiles) {
        this.people = people;
        this.companies = companies;
        this.posts = posts;
        this.postings = postings;
        this.graph = graph;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    SearchResults search(String rawQuery, boolean connectedOnly, Optional<Member> viewer) {
        String query = rawQuery == null ? "" : rawQuery.strip();
        Optional<String> tsquery = SearchTerms.prefixQuery(query);
        if (tsquery.isEmpty()) {
            // No browse-all door (§8.5): an empty or too-short query answers with
            // nothing at all, not with a first page of the membership.
            return SearchResults.unasked(query, connectedOnly, viewer.isPresent());
        }
        String asked = tsquery.get();
        return new SearchResults(query, connectedOnly, true, viewer.isPresent(),
                viewer.isEmpty(), peopleRows(asked, connectedOnly, viewer),
                companies.matching(asked, GROUP_LIMIT),
                posts.matching(asked, viewer, GROUP_LIMIT),
                postings.matching(asked, GROUP_LIMIT));
    }

    /**
     * §8.1's group. The tickable filter narrows the list the impersonal order
     * already produced and never touches that order — it is a fact about the
     * graph the seeker chose to apply, the same mechanism as the board's "roles
     * where I know someone", and emphatically not a weight applied on anyone's
     * behalf (§8.2, §5.6).
     */
    private List<SearchResults.PersonRow> peopleRows(String tsquery, boolean connectedOnly,
                                                     Optional<Member> viewer) {
        if (viewer.isEmpty()) {
            // §8.4's account gate. The seam refuses too — this is the page
            // declining to ask, so that the reason can be rendered instead.
            return List.of();
        }
        long viewerId = viewer.get().id();
        return people.named(tsquery, viewer, GROUP_LIMIT).stream()
                .filter(card -> !connectedOnly || graph.connected(viewerId, card.memberId()))
                .map(card -> new SearchResults.PersonRow(card, mutualNames(viewerId, card)))
                .toList();
    }

    private List<String> mutualNames(long viewerId, PersonCard card) {
        if (viewerId == card.memberId()) {
            return List.of();
        }
        return graph.mutuals(viewerId, card.memberId()).stream()
                .limit(MUTUALS_SHOWN)
                .map(profiles::cardFor)
                .map(PersonCard::displayName)
                .toList();
    }
}
