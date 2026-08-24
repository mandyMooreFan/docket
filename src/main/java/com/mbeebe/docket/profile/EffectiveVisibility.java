package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;

import java.util.Optional;

/**
 * Who may actually see a Profile — the Dial capped by the service-imposed floors
 * (§4.1), derived at read time and never stored (ADR-0002). Both floors are the same
 * shape, whole-profile and Dial-proof: an incomplete Profile (§3.2) and an under-18's
 * Profile (§9.2) are members-only and un-indexed no matter what the Dial says.
 */
public record EffectiveVisibility(Profile.Dial audience, boolean indexable) {

    static EffectiveVisibility of(Profile.Dial dial, boolean complete, boolean ownerIsMinor) {
        boolean floored = !complete || ownerIsMinor;
        Profile.Dial audience = floored && dial == Profile.Dial.PUBLIC
                ? Profile.Dial.MEMBERS_ONLY
                : dial;
        return new EffectiveVisibility(audience, audience == Profile.Dial.PUBLIC);
    }

    boolean visibleTo(long ownerId, Optional<Member> viewer, ConnectionLookup connections) {
        if (viewer.map(member -> member.id() == ownerId).orElse(false)) {
            return true;
        }
        return switch (audience) {
            case PUBLIC -> true;
            case MEMBERS_ONLY -> viewer.isPresent();
            case CONNECTIONS_ONLY -> viewer.isPresent()
                    && connections.connected(ownerId, viewer.get().id());
        };
    }
}
