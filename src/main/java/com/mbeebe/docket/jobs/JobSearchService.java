package com.mbeebe.docket.jobs;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Saved searches (§6.5): created explicitly from the current filter set —
 * never for you, never suggested — and stopped in one click, from the board or
 * from any mail's tokenized link, which works without login.
 */
@Service
class JobSearchService {

    private final JobSearchRepository searches;
    private final Clock clock;

    JobSearchService(JobSearchRepository searches, Clock clock) {
        this.searches = searches;
        this.clock = clock;
    }

    @Transactional
    void create(Member member, JobFilters filters, String frequency) {
        JobSearch.Frequency chosen;
        try {
            chosen = JobSearch.Frequency.valueOf(frequency);
        } catch (RuntimeException notAFrequency) {
            chosen = JobSearch.Frequency.WEEKLY;
        }
        Integer floor = null;
        if (!filters.floor().isBlank()) {
            try {
                floor = Integer.valueOf(filters.floor().strip());
            } catch (NumberFormatException ignored) {
                // A floor that isn't a number is no floor.
            }
        }
        JobPosting.RemotePolicy policy = null;
        if (!filters.remote().isBlank()) {
            try {
                policy = JobPosting.RemotePolicy.valueOf(filters.remote());
            } catch (RuntimeException ignored) {
                // Not a policy Docket knows: saved without one.
            }
        }
        searches.save(new JobSearch(member.id(), filters, floor, policy, chosen,
                Tokens.generate(), clock.instant()));
    }

    /** §6.5: the mail's one-click stop — no login, the token is the authority. */
    @Transactional
    boolean stopByToken(String token) {
        Optional<JobSearch> search = searches.findByStopToken(token);
        search.ifPresent(found -> found.stop(clock.instant()));
        return search.isPresent();
    }

    /** The board page's stop button — yours only. */
    @Transactional
    boolean stopOwn(Member member, long searchId) {
        Optional<JobSearch> search = searches.findByIdAndMemberId(searchId, member.id());
        search.ifPresent(found -> found.stop(clock.instant()));
        return search.isPresent();
    }

    @Transactional(readOnly = true)
    List<BoardPage.SavedSearchRow> listFor(long memberId) {
        return searches.findByMemberIdAndStoppedAtIsNullOrderByCreatedAt(memberId).stream()
                .map(search -> new BoardPage.SavedSearchRow(search.id(), describe(search),
                        search.frequency() == JobSearch.Frequency.DAILY ? "daily" : "weekly"))
                .toList();
    }

    private static String describe(JobSearch search) {
        JobFilters filters = search.filters();
        List<String> parts = new ArrayList<>();
        if (!filters.q().isBlank()) {
            parts.add("“" + filters.q() + "”");
        }
        if (!filters.location().isBlank()) {
            parts.add(filters.location());
        }
        if (!filters.remote().isBlank()) {
            parts.add(switch (filters.remote()) {
                case "ON_SITE" -> "on-site";
                case "HYBRID" -> "hybrid";
                default -> "remote";
            });
        }
        if (!filters.floor().isBlank()) {
            parts.add("from " + filters.floor() + " " + filters.currency());
        }
        if (!filters.company().isBlank()) {
            parts.add("at " + filters.company());
        }
        if (filters.known()) {
            parts.add("where you know someone");
        }
        return parts.isEmpty() ? "every new posting" : String.join(" · ", parts);
    }
}
