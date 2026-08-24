package com.mbeebe.docket.profile;

import com.mbeebe.docket.leaving.Departure;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * §11.2's first clause, in full: <em>your Profile goes.</em>
 *
 * <p>The whole row, not a blanking of its columns — name, headline, location,
 * summary, the Dial, the open-to-work choice, the photo pointer — plus every
 * Position, education entry and Skill hanging off it. Nothing survives that a
 * later reader could reassemble a person from.
 *
 * <p>Three consequences fall out rather than being coded, which is the reward for
 * ADR-0002 having been kept:
 *
 * <ul>
 *   <li><strong>Search.</strong> V9's {@code name_tsv} is a generated column on
 *       this row. The row goes, the index entry goes with it — there is no
 *       reindex to remember and no window in which a deleted member is still
 *       findable by name.
 *   <li><strong>The photo.</strong> Nulling {@code photo_image_id} would have been
 *       enough, and deleting the row does it: no Profile wears that image any
 *       more, so nobody claims it, and {@code images.ImageAudiences}' fail-closed
 *       default serves an unclaimed image to nobody. Exactly the rule a replaced
 *       photo already lived under (#52) — verified rather than assumed, by
 *       {@code LeavingTests}.
 *   <li><strong>Everything derived from the Profile.</strong> A Post rides its
 *       author's page (§5.4); a face rides the page it is on (§8.5). They do not
 *       need telling.
 * </ul>
 *
 * <p>The Profile page itself does not rely on any of that, and must not: a
 * Profile-less Member would otherwise fall through {@code Profile.blankFor} and
 * render an empty page to signed-in members. {@link ProfileService#pageFor} 404s
 * on the tombstone directly, which is the honest place for it — a former member's
 * page does not exist, and that is a fact about the Member, not about their rows.
 */
@Component
@Order(20)
class ProfileDeparture implements Departure {

    private final ProfileRepository profiles;
    private final PositionRepository positions;
    private final EducationRepository education;
    private final SkillRepository skills;

    ProfileDeparture(ProfileRepository profiles, PositionRepository positions,
                     EducationRepository education, SkillRepository skills) {
        this.profiles = profiles;
        this.positions = positions;
        this.education = education;
        this.skills = skills;
    }

    @Override
    @Transactional
    public void memberLeaving(long memberId) {
        positions.findByMemberIdOrderByStartMonthDesc(memberId).forEach(positions::delete);
        education.findByMemberIdOrderByCreatedAt(memberId).forEach(education::delete);
        skills.findByMemberIdOrderByCreatedAt(memberId).forEach(skills::delete);
        profiles.findById(memberId).ifPresent(profiles::delete);
    }
}
