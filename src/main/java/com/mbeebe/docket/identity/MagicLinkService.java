package com.mbeebe.docket.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Magic links are the whole auth system (§3.3): login and verification are the same
 * mechanism, there are no passwords, and the inbox is the recovery path.
 */
@Service
public class MagicLinkService {

    /** §3.3: rate-limited per address and per IP — anti-abuse for the mailer as much as anti-spam. */
    static final int MAX_PER_ADDRESS_PER_HOUR = 3;
    static final int MAX_PER_IP_PER_HOUR = 10;
    static final Duration LINK_LIFETIME = Duration.ofMinutes(30);

    private static final Pattern PLAUSIBLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public enum Outcome { SENT, INVALID_EMAIL, BLOCKED_DOMAIN, RATE_LIMITED }

    private final MagicLinkRepository magicLinks;
    private final LinkRequestRepository linkRequests;
    private final MemberRepository members;
    private final BlockedEmailDomains blockedDomains;
    private final Mailer mailer;
    private final Clock clock;
    private final String baseUrl;

    MagicLinkService(MagicLinkRepository magicLinks, LinkRequestRepository linkRequests,
                     MemberRepository members, BlockedEmailDomains blockedDomains,
                     Mailer mailer, Clock clock,
                     @Value("${docket.base-url:http://localhost:8080}") String baseUrl) {
        this.magicLinks = magicLinks;
        this.linkRequests = linkRequests;
        this.members = members;
        this.blockedDomains = blockedDomains;
        this.mailer = mailer;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    /**
     * The email step of joining. The declared age fact rides on the link so nothing
     * exists server-side until the link is used; an adult's arrives already reduced
     * to the bare kind — the month and year evaporated at the age screen (§3.1).
     */
    @Transactional
    public Outcome requestJoin(String email, Member.AgeKind ageKind, YearMonth birthIfMinor,
                               String requestIp) {
        return request(email, requestIp, tokenHash -> {
            Instant now = clock.instant();
            if (members.findByEmail(email).isPresent()) {
                // Already a Member: joining again is just signing in (§3.1).
                return MagicLink.login(tokenHash, email, requestIp, now, now.plus(LINK_LIFETIME));
            }
            return MagicLink.join(tokenHash, email, ageKind,
                    ageKind == Member.AgeKind.MINOR ? birthIfMinor : null,
                    requestIp, now, now.plus(LINK_LIFETIME));
        });
    }

    /**
     * Sign-in. An unknown address gets no mail and the same on-screen answer as a
     * known one — email lookup must not become a membership oracle (§8.3).
     */
    @Transactional
    public Outcome requestLogin(String email, String requestIp) {
        return request(email, requestIp, tokenHash -> {
            if (members.findByEmail(email).isEmpty()) {
                return null;
            }
            Instant now = clock.instant();
            return MagicLink.login(tokenHash, email, requestIp, now, now.plus(LINK_LIFETIME));
        });
    }

    private Outcome request(String email, String requestIp,
                            java.util.function.Function<String, MagicLink> linkFor) {
        if (!PLAUSIBLE_EMAIL.matcher(email).matches()) {
            return Outcome.INVALID_EMAIL;
        }
        if (blockedDomains.blocks(email)) {
            return Outcome.BLOCKED_DOMAIN;
        }
        Instant hourAgo = clock.instant().minus(Duration.ofHours(1));
        if (linkRequests.countByEmailAndCreatedAtAfter(email, hourAgo) >= MAX_PER_ADDRESS_PER_HOUR
                || linkRequests.countByRequestIpAndCreatedAtAfter(requestIp, hourAgo) >= MAX_PER_IP_PER_HOUR) {
            return Outcome.RATE_LIMITED;
        }
        // Every accepted request lands in the ledger, sent or not — limits that moved
        // only for real accounts would leak who has one (§8.3).
        linkRequests.save(new LinkRequest(email, requestIp, clock.instant()));
        String token = Tokens.generate();
        MagicLink link = linkFor.apply(Tokens.hash(token));
        if (link == null) {
            return Outcome.SENT;
        }
        magicLinks.save(link);
        String subject = link.purpose() == MagicLink.Purpose.JOIN ? "Join Docket" : "Sign in to Docket";
        mailer.send(email, subject, """
                Here is your link:

                %s/auth/%s

                It works once and expires in 30 minutes. If you didn't ask for it, \
                ignore this — nothing happens without the link.""".formatted(baseUrl, token));
        return Outcome.SENT;
    }

    /**
     * Consume a link: create the Member on first join (the moment the age fact is
     * stored, in its minimal form), or fetch them for a sign-in.
     */
    @Transactional
    public Optional<Member> consume(String token) {
        Instant now = clock.instant();
        return magicLinks.findByTokenHash(Tokens.hash(token))
                .filter(link -> link.usable(now))
                .map(link -> {
                    link.markUsed(now);
                    return members.findByEmail(link.email()).orElseGet(() -> {
                        LocalDate today = LocalDate.ofInstant(now, ZoneId.systemDefault());
                        return members.save(link.purpose() == MagicLink.Purpose.JOIN
                                && link.ageKind() == Member.AgeKind.MINOR
                                ? Member.minor(link.email(), link.birth(), today, now)
                                : Member.adult(link.email(), today, now));
                    });
                });
    }
}
