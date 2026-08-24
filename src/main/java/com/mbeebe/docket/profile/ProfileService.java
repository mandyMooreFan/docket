package com.mbeebe.docket.profile;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProfileService {

    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMM uuuu", Locale.UK);

    private final ProfileRepository profiles;
    private final PositionRepository positions;
    private final EducationRepository education;
    private final SkillRepository skills;
    private final Members members;
    private final Companies companies;
    private final ConnectionLookup connections;
    private final Clock clock;

    ProfileService(ProfileRepository profiles, PositionRepository positions,
                   EducationRepository education, SkillRepository skills, Members members,
                   Companies companies, ConnectionLookup connections, Clock clock) {
        this.profiles = profiles;
        this.positions = positions;
        this.education = education;
        this.skills = skills;
        this.members = members;
        this.companies = companies;
        this.connections = connections;
        this.clock = clock;
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
        List<PositionView> positionViews = positionViews(memberId);
        List<EducationView> educationViews = educationViews(memberId);
        Completeness completeness =
                Completeness.of(profile, positionViews.size(), educationViews.size());
        EffectiveVisibility visibility =
                EffectiveVisibility.of(profile.dial(), completeness.complete(), owner.get().isMinor());
        if (!visibility.visibleTo(memberId, viewer, connections)) {
            return Optional.empty();
        }
        boolean isOwner = viewer.map(member -> member.id() == memberId).orElse(false);
        return Optional.of(new ProfilePage(memberId, isOwner, profile.name(), profile.headline(),
                profile.location(), profile.summary(), initials(profile.name()),
                openToWorkShown(profile, memberId, viewer, isOwner), positionViews,
                educationViews, skillViews(memberId), completeness,
                profile.dial(), profile.openToWork(), visibility.indexable()));
    }

    /** The §3.2 bar, computed fresh from the stored facts — never cached, never stored. */
    @Transactional(readOnly = true)
    public Completeness completenessOf(long memberId) {
        Profile profile = profiles.findById(memberId).orElseGet(() -> Profile.blankFor(memberId));
        return Completeness.of(profile, positions.countByMemberId(memberId),
                education.countByMemberId(memberId));
    }

    /** What the edit page shows: your own facts, exactly as stored. */
    @Transactional
    public ProfileEdit editView(long memberId) {
        Profile profile = ownProfile(memberId);
        return new ProfileEdit(profile.name(), profile.headline(), profile.location(),
                profile.summary(), profile.dial(), profile.openToWork(), positionViews(memberId),
                educationViews(memberId), skillViews(memberId));
    }

    @Transactional
    public void editBasics(long memberId, String name, String headline, String location,
                           String summary) {
        ownProfile(memberId).editBasics(name, headline, location, summary);
    }

    @Transactional
    public void setDial(long memberId, Profile.Dial dial) {
        ownProfile(memberId).setDial(dial);
    }

    @Transactional
    public void setOpenToWork(long memberId, Profile.OpenToWork audience) {
        ownProfile(memberId).setOpenToWork(audience);
    }

    /** §6.1: naming an employer here is what brings a Company into being, or reuses it. */
    @Transactional
    public void addPosition(long memberId, String title, String companyName, YearMonth start,
                            String description) {
        Company company = companyName.isBlank() ? null : companies.named(companyName);
        positions.save(new Position(memberId, company, title.strip(), description.strip(),
                start, clock.instant()));
    }

    /** True when the position was the member's own to end; false is the caller's 404. */
    @Transactional
    public boolean endPosition(long memberId, long positionId, YearMonth end) {
        return positions.findByIdAndMemberId(positionId, memberId)
                .map(position -> {
                    position.endAt(end);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean deletePosition(long memberId, long positionId) {
        return positions.findByIdAndMemberId(positionId, memberId)
                .map(position -> {
                    positions.delete(position);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void addEducation(long memberId, String institution, String course,
                             Integer startYear, Integer endYear) {
        education.save(new EducationEntry(memberId, institution.strip(), course.strip(),
                startYear, endYear, clock.instant()));
    }

    @Transactional
    public boolean deleteEducation(long memberId, long entryId) {
        return education.findByIdAndMemberId(entryId, memberId)
                .map(entry -> {
                    education.delete(entry);
                    return true;
                })
                .orElse(false);
    }

    /** Adding a word you already declared is a quiet no-op, not an error. */
    @Transactional
    public void addSkill(long memberId, String name) {
        if (skills.findByMemberIdAndNameIgnoringCase(memberId, name.strip()).isEmpty()) {
            skills.save(new Skill(memberId, name.strip(), clock.instant()));
        }
    }

    @Transactional
    public boolean deleteSkill(long memberId, long skillId) {
        return skills.findByIdAndMemberId(skillId, memberId)
                .map(skill -> {
                    skills.delete(skill);
                    return true;
                })
                .orElse(false);
    }

    private List<EducationView> educationViews(long memberId) {
        return education.findByMemberIdOrderByCreatedAt(memberId).stream()
                .map(entry -> new EducationView(entry.id(), entry.institution(), entry.course(),
                        entry.years()))
                .toList();
    }

    private List<SkillView> skillViews(long memberId) {
        return skills.findByMemberIdOrderByCreatedAt(memberId).stream()
                .map(skill -> new SkillView(skill.id(), skill.name()))
                .toList();
    }

    private List<PositionView> positionViews(long memberId) {
        return positions.findByMemberIdOrderByStartMonthDesc(memberId).stream()
                .sorted(Comparator.comparing(Position::current).reversed())
                .map(position -> new PositionView(position.id(), position.title(),
                        position.company() == null ? "" : position.company().name(),
                        span(position.start(), position.end()), position.description(),
                        position.current()))
                .toList();
    }

    private static String span(YearMonth start, YearMonth end) {
        return MONTH.format(start) + " — " + (end == null ? "Present" : MONTH.format(end));
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
