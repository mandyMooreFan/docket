package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.company.CurrentPositions;
import com.mbeebe.docket.company.TrustGate;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The board's core (§6.3–6.5). Everything a viewer sees is derived at read
 * time from the stored facts (ADR-0002): open/closed from the window edge
 * against the clock, the §6.4 queue block from Outcome facts, capability from
 * Completeness plus the §6.2 trust gate — composed here, at the ask.
 */
@Service
class JobService {

    /** §6.3's fixed window: thirty days, then automatic close. */
    static final Duration WINDOW = Duration.ofDays(30);

    static final int MAX_TITLE = 200;
    static final int MAX_DESCRIPTION = 20_000;

    /** A refusal of the submitted content, with an honest reason — a 422. */
    static class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    /** A gate that did not pass — a 403 whose message reports the same check (§3.2). */
    static class NotAllowed extends RuntimeException {
        NotAllowed(String message) {
            super(message);
        }
    }

    private final JobPostingRepository postings;
    private final JobApplicationRepository applications;
    private final Companies companies;
    private final TrustGate trustGate;
    private final CurrentPositions positions;
    private final CapabilityService capabilities;
    private final ProfileService profiles;
    private final Clock clock;

    JobService(JobPostingRepository postings, JobApplicationRepository applications,
               Companies companies, TrustGate trustGate, CurrentPositions positions,
               CapabilityService capabilities, ProfileService profiles, Clock clock) {
        this.postings = postings;
        this.applications = applications;
        this.companies = companies;
        this.trustGate = trustGate;
        this.positions = positions;
        this.capabilities = capabilities;
        this.profiles = profiles;
        this.clock = clock;
    }

    /**
     * §6.3: post a job. The whole gate is asked here, in order — Completeness
     * (§3.2's POST_JOB), the §6.4 queue block, then the §6.2 trust gate for the
     * chosen Company — before a word of the content is judged.
     */
    @Transactional
    long post(Member poster, String companyId, String title, String location,
              String remotePolicy, String salaryMin, String salaryMax, String currency,
              String description) {
        if (capabilities.may(poster.id(), Capability.POST_JOB) != CapabilityAnswer.YES) {
            throw new NotAllowed("Posting a job opens when your profile is complete.");
        }
        blockReason(poster.id()).ifPresent(reason -> {
            throw new NotAllowed(reason);
        });
        Company company = parseLong(companyId).flatMap(companies::findResolved)
                .orElseThrow(() -> new Refused("Choose the company this role is at."));
        if (!trustGate.passes(poster.id(), company.id())) {
            throw new NotAllowed("Posting for " + company.name() + " needs a current position "
                    + "there on your profile and a verified work address at its mail domain.");
        }

        String cleanTitle = title == null ? "" : title.strip();
        if (cleanTitle.isEmpty()) {
            throw new Refused("A posting needs a title.");
        }
        if (cleanTitle.length() > MAX_TITLE) {
            throw new Refused("A title can hold at most 200 characters.");
        }
        String cleanDescription = description == null ? "" : description.strip();
        if (cleanDescription.isEmpty()) {
            throw new Refused("A posting needs a description.");
        }
        if (cleanDescription.length() > MAX_DESCRIPTION) {
            throw new Refused("A description can hold at most 20,000 characters.");
        }
        JobPosting.RemotePolicy policy = parsePolicy(remotePolicy)
                .orElseThrow(() -> new Refused("Say whether the role is on-site, hybrid or remote."));
        // §6.3: the mandatory real salary range. No "competitive", no single number.
        int min = parseLong(salaryMin).filter(v -> v > 0 && v <= Integer.MAX_VALUE)
                .map(Long::intValue)
                .orElseThrow(() -> new Refused(
                        "A posting needs a real salary range — a smallest and a largest number."));
        int max = parseLong(salaryMax).filter(v -> v > 0 && v <= Integer.MAX_VALUE)
                .map(Long::intValue)
                .orElseThrow(() -> new Refused(
                        "A posting needs a real salary range — a smallest and a largest number."));
        if (min >= max) {
            throw new Refused("A salary range is two different numbers, smallest first.");
        }
        if (!Salaries.known(currency)) {
            throw new Refused("Choose one of the currencies Docket knows.");
        }

        Instant now = clock.instant();
        return postings.save(new JobPosting(company.id(), poster.id(), cleanTitle,
                cleanDescription, location == null ? "" : location.strip(), policy,
                min, max, currency, now, now.plus(WINDOW))).id();
    }

    /**
     * §6.4's block, in one sentence the poster can act on. The rule: any
     * Application on any of your postings that closed without response and has
     * still not been given an Outcome blocks a new posting — one is enough,
     * because the remedy is two clicks per applicant and the guarantee is owed
     * to each of them singly.
     */
    @Transactional(readOnly = true)
    Optional<String> blockReason(long posterId) {
        List<JobApplication> neglected = applications.neglectedOnPostingsOf(posterId);
        if (neglected.isEmpty()) {
            return Optional.empty();
        }
        String titles = neglected.stream()
                .map(JobApplication::postingId).distinct()
                .map(id -> "“" + postings.findById(id).orElseThrow().title() + "”")
                .collect(Collectors.joining(", "));
        return Optional.of("You can post a new job when you've given an outcome to the "
                + "applications that closed without response on " + titles + ".");
    }

    /** The Companies this member may post for right now — the §6.2 gate, listed. */
    @Transactional(readOnly = true)
    List<CompanyOption> postableCompanies(long memberId) {
        return positions.companiesHeldBy(memberId).stream()
                .flatMap(companyId -> companies.findResolved(companyId).stream())
                .distinct()
                .filter(company -> trustGate.passes(memberId, company.id()))
                .map(company -> new CompanyOption(company.id(), company.name()))
                .toList();
    }

    record CompanyOption(long id, String name) {
    }

    /** §6.5: one list, newest first, no ranking. Filters narrow; nothing reorders. */
    @Transactional(readOnly = true)
    BoardPage board(JobFilters filters, Optional<Member> viewer) {
        List<PostingRow> rows = postings.openAt(clock.instant()).stream()
                .map(this::row)
                .toList();
        return new BoardPage(rows, filters, viewer.isPresent(), List.of());
    }

    /** The posting page for anyone, signed in or out (§8.4); empty is the 404. */
    @Transactional(readOnly = true)
    Optional<PostingPage> postingPage(long id, Optional<Member> viewer) {
        return postings.findById(id).map(posting -> {
            boolean open = posting.openAt(clock.instant());
            Company company = companies.findResolved(posting.companyId()).orElseThrow();
            boolean mayShare = open && viewer.isPresent() && capabilities
                    .may(viewer.get().id(), Capability.POST) == CapabilityAnswer.YES;
            return new PostingPage(posting.id(), posting.title(),
                    Salaries.line(posting.salaryMin(), posting.salaryMax(), posting.currency()),
                    placeLine(posting), company.name(), company.id(), posting.description(),
                    profiles.cardFor(posting.posterId()), day(posting.postedAt()),
                    day(posting.closesAt()), open, applyBox(posting, open, viewer), mayShare);
        });
    }

    /**
     * §3.2: the apply button "simply reports the same check" — every refusal the
     * POST would make is said here, in the same words the member can act on.
     */
    private PostingPage.ApplyBox applyBox(JobPosting posting, boolean open,
                                          Optional<Member> viewer) {
        if (viewer.isEmpty()) {
            return box(open ? PostingPage.ApplyBox.Kind.SIGNED_OUT
                    : PostingPage.ApplyBox.Kind.CLOSED);
        }
        Member member = viewer.get();
        if (member.id() == posting.posterId()) {
            return box(PostingPage.ApplyBox.Kind.YOURS);
        }
        Optional<JobApplication> applied =
                applications.findByPostingIdAndApplicantId(posting.id(), member.id());
        if (applied.isPresent()) {
            return new PostingPage.ApplyBox(PostingPage.ApplyBox.Kind.APPLIED,
                    stateLabel(applied.get().state()), day(applied.get().appliedAt()),
                    List.of());
        }
        if (!open) {
            return box(PostingPage.ApplyBox.Kind.CLOSED);
        }
        var completeness = profiles.completenessOf(member.id());
        if (!completeness.complete()) {
            return new PostingPage.ApplyBox(PostingPage.ApplyBox.Kind.INCOMPLETE, "", "",
                    completeness.missing());
        }
        return box(PostingPage.ApplyBox.Kind.OPEN);
    }

    private static PostingPage.ApplyBox box(PostingPage.ApplyBox.Kind kind) {
        return new PostingPage.ApplyBox(kind, "", "", List.of());
    }

    static String stateLabel(JobApplication.State state) {
        return switch (state) {
            case RECEIVED -> "Received";
            case ADVANCED -> "Advanced";
            case NOT_SELECTED -> "Not selected";
            case CLOSED_WITHOUT_RESPONSE -> "Closed without response";
        };
    }

    PostingRow row(JobPosting posting) {
        String companyName = companies.findResolved(posting.companyId())
                .map(Company::name).orElse("");
        return new PostingRow(posting.id(), posting.title(), posting.companyId(), companyName,
                Salaries.line(posting.salaryMin(), posting.salaryMax(), posting.currency()),
                placeLine(posting), day(posting.postedAt()));
    }

    static String placeLine(JobPosting posting) {
        String policy = switch (posting.remotePolicy()) {
            case ON_SITE -> "On-site";
            case HYBRID -> "Hybrid";
            case REMOTE -> "Remote";
        };
        return posting.location().isBlank() ? policy : policy + " — " + posting.location();
    }

    String day(Instant instant) {
        return DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
                .withZone(clock.getZone()).format(instant);
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value.strip()));
        } catch (RuntimeException notANumber) {
            return Optional.empty();
        }
    }

    private static Optional<JobPosting.RemotePolicy> parsePolicy(String value) {
        try {
            return Optional.of(JobPosting.RemotePolicy.valueOf(value));
        } catch (RuntimeException notAPolicy) {
            return Optional.empty();
        }
    }
}
