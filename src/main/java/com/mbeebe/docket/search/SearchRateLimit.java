package com.mbeebe.docket.search;

import com.mbeebe.docket.identity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * §10.3: "scraping is answered by rate limits on search — per account; per IP
 * logged out — a system control applied to everyone, not moderation, so it never
 * enters the queue or the ladder."
 *
 * <p>Everything about this class follows from that sentence. It is applied to
 * every asker identically, it consults no moderation state and writes none, it
 * has no ladder rung and no appeal, and hitting it changes nothing about the
 * member's account: the next window opens on the clock, not on a decision.
 *
 * <p>The numbers are human-scale on purpose. Sixty searches in ten minutes is
 * one every ten seconds sustained — far past anyone reading results, far under
 * a scraper walking the alphabet. Signed out the address is all there is to
 * count, so it is tighter, and the highest-value door is closed anyway: people
 * search needs an account (§8.4).
 *
 * <p>Only searches that actually run are counted. A query with nothing in it to
 * ask (§8.5's floor, see {@link SearchTerms}) does no work and costs no budget,
 * which also means the limit can never be spent by a page that renders no
 * results.
 */
@Service
class SearchRateLimit {

    static final Duration WINDOW = Duration.ofMinutes(10);
    static final int PER_MEMBER = 60;
    static final int PER_IP_SIGNED_OUT = 30;

    private final SearchRequestRepository requests;
    private final Clock clock;

    SearchRateLimit(SearchRequestRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    /**
     * True when this search may run — and, when it may, records it. False is a
     * plain "not right now": no fact about the asker changes either way.
     */
    @Transactional
    boolean accept(Optional<Member> viewer, String requestIp) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(WINDOW);
        if (viewer.isPresent()) {
            long memberId = viewer.get().id();
            if (requests.countByMemberIdAndCreatedAtAfter(memberId, cutoff) >= PER_MEMBER) {
                return false;
            }
            requests.save(SearchRequest.by(memberId, now));
            return true;
        }
        String address = requestIp == null ? "" : requestIp;
        if (requests.countByRequestIpAndCreatedAtAfter(address, cutoff) >= PER_IP_SIGNED_OUT) {
            return false;
        }
        requests.save(SearchRequest.from(address, now));
        return true;
    }
}
