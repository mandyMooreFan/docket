package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.moderation.ReportableContent;
import com.mbeebe.docket.moderation.TargetKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The profile module's answer to {@link ReportableContent} (§10.2, §10.3): the
 * PROFILE kind, and only that one. §10.2 reaches a Member through the Profile
 * they publish, which is why there is no MEMBER kind for this to answer for.
 *
 * <p>{@link #visibleToReporter} is {@link ProfileService#pageFor} with the
 * viewer, and deliberately nothing more: the Dial, §4.1's floors, §7.3's Blocks
 * and §10.3's own removal are all already in that one answer, and a Profile out
 * of its audience is not reportable for the same reason it is a 404.
 *
 * <p>Existence is the Member's, not the row's: there is exactly one Profile per
 * Member from the moment they join (CONTEXT.md), so a Member who has never
 * touched theirs still has a page to report and the row is written on the way to
 * removing it. The alternative — "no row, nothing to remove" — is the silent
 * success the registry's fail-closed default exists to prevent.
 *
 * <p>The item's own author is its subject: a Profile is the page a Member
 * publishes about themselves, so there is exactly one person answerable for it
 * and the ladder's member-facing rungs know who to apply to.
 */
@Component
class ProfileReportable implements ReportableContent {

    private final ProfileRepository profiles;
    private final ProfileService service;
    private final Members members;

    ProfileReportable(ProfileRepository profiles, ProfileService service, Members members) {
        this.profiles = profiles;
        this.service = service;
        this.members = members;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> visibleToReporter(TargetKind kind, long id,
                                                    Optional<Member> viewer) {
        if (kind != TargetKind.PROFILE || service.pageFor(id, viewer).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(item(asStored(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> forModeration(TargetKind kind, long id) {
        if (kind != TargetKind.PROFILE || members.find(id).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(item(asStored(id)));
    }

    @Override
    @Transactional
    public boolean remove(TargetKind kind, long id, Instant now) {
        if (kind != TargetKind.PROFILE) {
            return false;
        }
        members.find(id).ifPresent(member -> service.ownProfile(id).remove(now));
        return true;
    }

    @Override
    @Transactional
    public boolean restore(TargetKind kind, long id) {
        if (kind != TargetKind.PROFILE) {
            return false;
        }
        profiles.findById(id).ifPresent(Profile::restore);
        return true;
    }

    private Profile asStored(long memberId) {
        return profiles.findById(memberId).orElseGet(() -> Profile.blankFor(memberId));
    }

    /**
     * The words the Member wrote about themselves, which is what a Report about a
     * Profile is nearly always about. Positions, education and Skills are on the
     * page the href points at; the queue judges against the real thing.
     */
    private static ReportedItem item(Profile profile) {
        String summary = (profile.name() + "\n" + profile.headline() + "\n"
                + profile.location() + "\n\n" + profile.summary()).strip();
        return new ReportedItem(TargetKind.PROFILE, profile.memberId(),
                Optional.of(profile.memberId()), summary,
                "/p/" + profile.memberId(), profile.removed());
    }
}
