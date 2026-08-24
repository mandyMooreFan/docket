package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final Members members;
    private final ConnectionLookup connections;

    ProfileService(ProfileRepository profiles, Members members, ConnectionLookup connections) {
        this.profiles = profiles;
        this.members = members;
        this.connections = connections;
    }

    @Transactional
    Profile ownProfile(long memberId) {
        return profiles.findById(memberId)
                .orElseGet(() -> profiles.save(Profile.blankFor(memberId)));
    }

    /**
     * The Profile page as the viewer is allowed to see it; empty when they are not —
     * a Profile out of its audience does not exist, with no placeholder (§9.4's shape).
     */
    @Transactional(readOnly = true)
    public Optional<ProfilePage> pageFor(long memberId, Optional<Member> viewer) {
        Optional<Member> owner = members.find(memberId);
        if (owner.isEmpty()) {
            return Optional.empty();
        }
        Profile profile = profiles.findById(memberId).orElseGet(() -> Profile.blankFor(memberId));
        Completeness completeness = Completeness.of(profile, 0, 0);
        EffectiveVisibility visibility =
                EffectiveVisibility.of(profile.dial(), completeness.complete(), owner.get().isMinor());
        if (!visibility.visibleTo(memberId, viewer, connections)) {
            return Optional.empty();
        }
        boolean isOwner = viewer.map(member -> member.id() == memberId).orElse(false);
        return Optional.of(new ProfilePage(memberId, isOwner, profile.name(), profile.headline(),
                profile.location(), profile.summary(), initials(profile.name()),
                openToWorkShown(profile, memberId, viewer, isOwner), completeness, profile.dial(),
                profile.openToWork(), visibility.indexable()));
    }

    /** What the edit page shows: your own facts, exactly as stored. */
    @Transactional
    public ProfileEdit editView(long memberId) {
        Profile profile = ownProfile(memberId);
        return new ProfileEdit(profile.name(), profile.headline(), profile.location(),
                profile.summary(), profile.dial(), profile.openToWork());
    }

    @Transactional
    public void editBasics(long memberId, String name, String headline, String location,
                           String summary) {
        ownProfile(memberId).editBasics(name, headline, location, summary);
    }

    /** The quiet flag renders only inside its chosen audience — and never logged-out. */
    private boolean openToWorkShown(Profile profile, long ownerId, Optional<Member> viewer,
                                    boolean isOwner) {
        if (profile.openToWork() == Profile.OpenToWork.OFF || viewer.isEmpty()) {
            return false;
        }
        return isOwner || switch (profile.openToWork()) {
            case MEMBERS -> true;
            case CONNECTIONS -> connections.connected(ownerId, viewer.get().id());
            case OFF -> false;
        };
    }

    static String initials(String name) {
        String[] words = name.strip().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "·";
        }
        String first = words[0].substring(0, 1);
        String last = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }
}
