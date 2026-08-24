package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.search.PeopleSearch;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The profile module's answer to {@link PeopleSearch} (§8.1): name matches,
 * narrowed by the Profile page's own derivations, in an order that is about the
 * text and never about the asker.
 *
 * <p>Logged out this returns nothing at all, whatever the query — §8.4's
 * account gate, and the honest explanation for it is the results page's job
 * (§13.4: say what the empty part is, do not pretend nobody matched).
 */
@Component
class PeopleByName implements PeopleSearch {

    /**
     * FTS narrows, then §8.5 narrows again — so more candidates are fetched
     * than rows are shown, or a page of hidden Profiles would silently swallow
     * the visible ones behind them. Bounded, because an unbounded fan-out is an
     * enumeration surface with extra steps.
     */
    private static final int CANDIDATE_FACTOR = 5;

    private final ProfileRepository profiles;
    private final ProfileService service;

    PeopleByName(ProfileRepository profiles, ProfileService service) {
        this.profiles = profiles;
        this.service = service;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonCard> named(String tsquery, Optional<Member> viewer, int limit) {
        if (viewer.isEmpty()) {
            return List.of();
        }
        return service.searchableAmong(
                        profiles.nameCandidates(tsquery, limit * CANDIDATE_FACTOR), viewer)
                .stream()
                .limit(limit)
                .toList();
    }
}
