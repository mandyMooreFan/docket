package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;

import java.util.List;

/**
 * How the Profile page asks the feed module (#33) for its §5.4 dated Posts
 * list — the same one-interface seam as {@link ProfileGraphLookup}. Called only
 * after the page itself passed visibility, so the lookup adds just the
 * per-item §9.4 rule (minor-authored Posts omitted logged-out, no placeholder).
 */
public interface ProfilePostsLookup {

    List<ProfilePostView> postsOn(long ownerId, java.util.Optional<Member> viewer);
}
