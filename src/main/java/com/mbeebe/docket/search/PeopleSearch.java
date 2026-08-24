package com.mbeebe.docket.search;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.PersonCard;

import java.util.List;
import java.util.Optional;

/**
 * What search asks the profile module for §8.1's people group — the same
 * seam shape as {@link com.mbeebe.docket.profile.ProfilePostsLookup}, pointing
 * the other way, and implemented in com.mbeebe.docket.profile so the Dial, the
 * §3.2/§9.2 floors and Blocks are decided by the code that owns them.
 *
 * <p>The answer is a list of {@link PersonCard}s and nothing else: no score, no
 * distance, no reason. There is deliberately no word for relevance-to-you, and
 * nothing in this interface could carry one.
 */
public interface PeopleSearch {

    /**
     * Members whose NAME matches, in impersonal order (§8.2: textual match
     * quality then a stable id tiebreak, never a function of who is asking),
     * already narrowed to what this viewer may be shown (§8.5).
     *
     * <p>Empty for a viewer who is not signed in — §8.4 account-gates people
     * search, which is the single highest-value anti-scraping rule available.
     */
    List<PersonCard> named(String tsquery, Optional<Member> viewer, int limit);
}
