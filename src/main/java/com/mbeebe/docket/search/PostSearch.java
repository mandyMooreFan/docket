package com.mbeebe.docket.search;

import com.mbeebe.docket.identity.Member;

import java.util.List;
import java.util.Optional;

/**
 * What search asks the feed module (§8.4: Posts are searchable logged out, with
 * §9.4's exclusion of anything authored by an under-18). Implemented in
 * com.mbeebe.docket.feed, which reuses the Post page's own visibleTo derivation
 * wholesale rather than restating it here.
 *
 * <p>The author arrives as a NAME, not a member id, and that is deliberate: a
 * Post row therefore cannot carry a link into a Profile, so the results page has
 * exactly one surface that hands back a set of people — the account-gated,
 * name-matched People group. §8.5's no-enumeration rule is structural rather
 * than remembered.
 */
public interface PostSearch {

    List<Hit> matching(String tsquery, Optional<Member> viewer, int limit);

    record Hit(long id, String excerpt, String author, String when) {
    }
}
