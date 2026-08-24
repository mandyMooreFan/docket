package com.mbeebe.docket.profile;

import com.mbeebe.docket.leaving.ExportContributor;
import com.mbeebe.docket.leaving.ExportField;
import com.mbeebe.docket.leaving.ExportMedia;
import com.mbeebe.docket.leaving.ExportRecord;
import com.mbeebe.docket.leaving.ExportSection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The Profile's slice of the archive (§11.1): the page the member publishes, as
 * stored — the whole of it, including the photo.
 *
 * <p>The Dial and the open-to-work audience are in here because they are stored
 * <em>choices</em>, not conclusions. Effective visibility — what the Dial actually
 * comes to once §3.2's and §9.2's floors have had their say — is not, and cannot
 * be: it is derived at every read and written down nowhere (ADR-0002), which puts
 * it outside Article 20 as a category (WP242 p.10). The archive says so in its
 * supplementary information rather than leaving the member to wonder where their
 * "who can see me" answer went.
 *
 * <p>Completeness is absent for the same reason, and the member loses nothing: the
 * three facts it is computed from — a name, a headline, a Position or an education
 * entry — are all right here.
 */
@Component
@Order(20)
class ProfileExport implements ExportContributor {

    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK);

    private final ProfileRepository profiles;
    private final PositionRepository positions;
    private final EducationRepository education;
    private final SkillRepository skills;

    ProfileExport(ProfileRepository profiles, PositionRepository positions,
                  EducationRepository education, SkillRepository skills) {
        this.profiles = profiles;
        this.positions = positions;
        this.education = education;
        this.skills = skills;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        return List.of(profile(memberId), positions(memberId), education(memberId),
                skills(memberId));
    }

    private ExportSection profile(long memberId) {
        Optional<Profile> found = profiles.findById(memberId);
        if (found.isEmpty()) {
            return ExportSection.of("profile", "Your profile", List.of());
        }
        Profile profile = found.get();
        List<ExportField> fields = List.of(
                ExportField.of("name", "Name", profile.name()),
                ExportField.of("headline", "Headline", profile.headline()),
                ExportField.of("location", "Location", profile.location()),
                ExportField.of("summary", "Summary", profile.summary()),
                ExportField.of("dial", "Who can see your profile", dial(profile.dial())),
                ExportField.of("open_to_work", "Open to work",
                        openToWork(profile.openToWork())));
        // §4.1's photo: the bytes, not the pointer. leaving.Archive fetches them
        // through the same /images/{id} audience guard that would hand them to
        // this member on request, so nothing here reaches past what they can
        // already see (§8.5).
        List<ExportMedia> media = profile.photoImageId() == null
                ? List.of()
                : List.of(ExportMedia.of(profile.photoImageId(), "photo"));
        return ExportSection.of("profile", "Your profile",
                List.of(ExportRecord.of("", fields, media)));
    }

    private ExportSection positions(long memberId) {
        return ExportSection.of("positions", "Your positions",
                positions.findByMemberIdOrderByStartMonthDesc(memberId).stream()
                        .map(position -> ExportRecord.of(position.title(), List.of(
                                ExportField.of("title", "Title", position.title()),
                                ExportField.of("company", "Company",
                                        position.company() == null
                                                ? "" : position.company().name()),
                                ExportField.of("from", "From", month(position.start())),
                                ExportField.of("to", "To",
                                        position.end() == null
                                                ? "Present" : month(position.end())),
                                ExportField.of("description", "Description",
                                        position.description()))))
                        .toList());
    }

    private ExportSection education(long memberId) {
        return ExportSection.of("education", "Your education",
                education.findByMemberIdOrderByCreatedAt(memberId).stream()
                        .map(entry -> ExportRecord.of(entry.institution(), List.of(
                                ExportField.of("institution", "Institution",
                                        entry.institution()),
                                ExportField.of("course", "Course", entry.course()),
                                ExportField.of("years", "Years", entry.years()))))
                        .toList());
    }

    /** §4.1: words you declared. Nobody attested to any of them, so nobody is named. */
    private ExportSection skills(long memberId) {
        List<ExportRecord> records = new ArrayList<>();
        for (Skill skill : skills.findByMemberIdOrderByCreatedAt(memberId)) {
            records.add(ExportRecord.of("",
                    List.of(ExportField.of("skill", "Skill", skill.name()))));
        }
        return ExportSection.of("skills", "Your skills", records);
    }

    private static String month(YearMonth month) {
        return month == null ? "" : MONTH.format(month);
    }

    private static String dial(Profile.Dial dial) {
        return switch (dial) {
            case PUBLIC -> "Anyone, including people who are not signed in";
            case MEMBERS_ONLY -> "Signed-in members";
            case CONNECTIONS_ONLY -> "Your connections";
        };
    }

    private static String openToWork(Profile.OpenToWork audience) {
        return switch (audience) {
            case OFF -> "Off";
            case CONNECTIONS -> "Shown to your connections";
            case MEMBERS -> "Shown to signed-in members";
        };
    }
}
