package com.mbeebe.docket.profile;

import java.util.ArrayList;
import java.util.List;

/**
 * The §3.2 bar, derived on every read and never stored (ADR-0002): verified email —
 * inherent, since every Member signed up through a magic link — plus a name, a headline,
 * and at least one Position or education entry. Deliberately not a photo, not a summary,
 * not an accepted Connection.
 */
public record Completeness(boolean named, boolean headlined, boolean hasEntry) {

    static Completeness of(Profile profile, long positionCount, long educationCount) {
        return new Completeness(!profile.name().isBlank(), !profile.headline().isBlank(),
                positionCount + educationCount > 0);
    }

    public boolean complete() {
        return named && headlined && hasEntry;
    }

    /** What is still missing, in the words the owner should read (§3.2's bar). */
    public List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (!named) {
            missing.add("your name");
        }
        if (!headlined) {
            missing.add("a headline");
        }
        if (!hasEntry) {
            missing.add("a position or education entry");
        }
        return missing;
    }
}
