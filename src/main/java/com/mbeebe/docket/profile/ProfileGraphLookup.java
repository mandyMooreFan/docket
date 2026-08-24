package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;

import java.util.Optional;

/** How the Profile page asks the graph module (#32) for its §4.2–4.3 section. */
public interface ProfileGraphLookup {

    ProfileGraph onProfile(long ownerId, Optional<Member> viewer);
}
