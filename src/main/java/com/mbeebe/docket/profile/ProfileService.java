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
        // §7.3: a Block is total — for each of the pair, the other's Profile does
        // not exist, with the same no-placeholder 404 as any missing page.
        if (viewer.isPresent() && viewer.get().id() != memberId
                && connections.blocked(memberId, viewer.get().id())) {
            return Optional.empty();
        }
        Profile profile = profiles.findById(memberId).orElseGet(() -> Profile.blankFor(memberId));
        // §10.3 rung 1, and unlike the Block check this one has no self-exception:
        // a removed Profile does not exist for anybody, its owner included. The
        // Member is untouched — signing in and the edit page both still work, which
        // is what keeps §10.3's "the member is told which state they are in" and
        // §10.5's reversibility possible. Every surface that asks this question —
        // people search, the Company page's people list, a Post's visibility —
        // inherits the answer without restating it.
        if (profile.removed()) {
            return Optional.empty();
        }
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
                profile.dial(), profile.openToWork(), visibility.indexable(),
                profile.photoImageId()));
    }

    /**
     * §6.3's one deliberate exception to the single Dial: applying hands the
     * poster a full view of the Profile FOR THAT APPLICATION, whatever the Dial
     * — consent given by the act of applying, which is why this must not weaken
     * {@link #pageFor}: the Dial, the floors and Blocks all stay untouched on
     * every other surface, and only the jobs module's application queue renders
     * this view, scoped to one Application. What it deliberately does NOT carry:
     * the member's authored feed content (the §9.4 caps bind Posts and Replies,
     * which are not the Profile and are not here), the open-to-work flag (it
     * rides its own audience, and an application already says more), and any
     * graph affordance. Empty when the Member does not exist — or when the Profile
     * has been removed (§10.3 rung 1), which needs saying separately here precisely
     * because this method bypasses the Dial and the floors: a bypass that did not
     * also ask about removal would be the one surface a removed Profile still
     * rendered on.
     */
    @Transactional(readOnly = true)
    public Optional<ProfilePage> pageForApplication(long memberId) {
        if (members.find(memberId).isEmpty()) {
            return Optional.empty();
        }
        Profile profile = profiles.findById(memberId).orElseGet(() -> Profile.blankFor(memberId));
        if (profile.removed()) {
            return Optional.empty();
        }
        List<PositionView> positionViews = positionViews(memberId);
        List<EducationView> educationViews = educationViews(memberId);
        Completeness completeness =
                Completeness.of(profile, positionViews.size(), educationViews.size());
        return Optional.of(new ProfilePage(memberId, false, profile.name(), profile.headline(),
                profile.location(), profile.summary(), initials(profile.name()), false,
                positionViews, educationViews, skillViews(memberId), completeness,
                profile.dial(), profile.openToWork(), false, profile.photoImageId()));
    }

    /**
     * §8.5, in one place: which of these Members people search may return, in
     * the order they were handed in (§8.2 — nothing here reorders anything).
     *
     * <p>Three gates, and the first is simply {@link #pageFor}: a result may
     * never exceed the Dial, so the question "may search show this person" is
     * answered by asking for their page as this viewer and seeing whether one
     * comes back. Blocks, the Dial and both floors come along for free, derived
     * fresh, so a Dial turned down takes effect on the next query.
     *
     * <p>On top of that, the two floors' own subjects are absent whatever the
     * Dial says and whoever is asking: an under-18's Profile is never returned
     * by people search (§8.1, §9.2), and an incomplete Profile stays un-indexed
     * (§3.2, §8.5). Both are service-imposed and Dial-proof — a minor who sets
     * their Dial to public is still not findable by name, and neither is a
     * member who has typed nothing but their name.
     */
    @Transactional(readOnly = true)
    public List<PersonCard> searchableAmong(List<Long> candidates, Optional<Member> viewer) {
        return candidates.stream()
                .filter(this::adultAndComplete)
                .filter(memberId -> pageFor(memberId, viewer).isPresent())
                .map(this::cardFor)
                .toList();
    }

    private boolean adultAndComplete(long memberId) {
        return members.find(memberId).map(owner -> !owner.isMinor()).orElse(false)
                && completenessOf(memberId).complete();
    }

    /**
     * How lists elsewhere point at a Member (#32's /network, Mutuals): a card, never
     * the entity. The card carries the photo's ADDRESS, not the bytes and not a
     * decision — /images/{id} re-derives, per viewer and per request, whether that
     * address answers (§8.5). A card whose photo is out of the reader's reach draws
     * the initials it always carries.
     */
    @Transactional(readOnly = true)
    public PersonCard cardFor(long memberId) {
        Optional<Profile> profile = profiles.findById(memberId);
        String name = profile.map(Profile::name).orElse("");
        return new PersonCard(memberId, name, initials(name),
                profile.map(Profile::photoImageId).orElse(null));
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
                educationViews(memberId), skillViews(memberId), initials(profile.name()),
                profile.photoImageId());
    }

    /**
     * The §4.1 photo, set or cleared. Passing null is removal, and that symmetry is
     * the point: taking your face off Docket is exactly as many clicks as putting it
     * on. The bytes reached the one image store (§10.4) before this is called — this
     * only records which stored image a Profile currently wears.
     *
     * <p>Nothing else changes. A photo is not on the §3.2 bar, so setting one earns
     * no Capability and removing one costs none.
     */
    @Transactional
    public void setPhoto(long memberId, Long imageId) {
        ownProfile(memberId).setPhoto(imageId);
    }

    /** The signed-in member's own photo, for the app-bar avatar (§2's layout). */
    @Transactional(readOnly = true)
    public Optional<Long> photoOf(long memberId) {
        return profiles.findById(memberId).map(Profile::photoImageId);
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

    /**
     * The ended position's view when it was the member's own to end — the
     * caller needs its title and company for §5.2.3's opt-in share; empty is
     * the caller's 404.
     */
    @Transactional
    public Optional<PositionView> endPosition(long memberId, long positionId, YearMonth end) {
        return positions.findByIdAndMemberId(positionId, memberId)
                .map(position -> {
                    position.endAt(end);
                    return new PositionView(position.id(), position.title(),
                            nameOf(position.company()), idOf(position.company()),
                            span(position.start(), end), position.description(), false);
                });
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
                        nameOf(position.company()), idOf(position.company()),
                        span(position.start(), position.end()), position.description(),
                        position.current()))
                .toList();
    }

    /**
     * §10.3 rung 1 reaching the Position's employer line: a removed Company is not
     * named and not linked, so the entry renders as the role alone — the same shape
     * a Position that never named an employer already has. The Position itself is
     * the Member's own claim and stays; only the Company stops rendering.
     */
    private static String nameOf(Company company) {
        return company == null || company.removed() ? "" : company.name();
    }

    private static Long idOf(Company company) {
        return company == null || company.removed() ? null : company.id();
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
