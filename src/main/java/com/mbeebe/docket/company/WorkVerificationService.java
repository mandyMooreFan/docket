package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.Mailer;
import com.mbeebe.docket.identity.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The Work verification flow (§6.2): a magic link to an address at the Company's
 * domain, the same machinery as login. Identity's rules govern the LOGIN address;
 * this is a second address, so the alias/blocklist rules deliberately do not apply
 * here — what matters is only that the mail actually arrives at the domain.
 */
@Service
class WorkVerificationService {

    /** §3.3's posture for the third outbound-mail source: limited per address and per member. */
    static final int MAX_PER_ADDRESS_PER_HOUR = 3;
    static final int MAX_PER_MEMBER_PER_HOUR = 5;
    static final Duration LINK_LIFETIME = Duration.ofMinutes(30);

    private static final Pattern PLAUSIBLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+$");
    /** A real-looking mail domain: dotted labels and a lettered top level. */
    private static final Pattern PLAUSIBLE_DOMAIN =
            Pattern.compile("^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$");

    enum Outcome { SENT, INVALID_ADDRESS, RATE_LIMITED, NOT_ELIGIBLE }

    private final WorkLinkRepository links;
    private final WorkLinkRequestRepository ledger;
    private final WorkVerificationRepository verifications;
    private final CurrentPositions positions;
    private final Companies companies;
    private final CompanyMerges merges;
    private final Mailer mailer;
    private final Clock clock;
    private final String baseUrl;

    WorkVerificationService(WorkLinkRepository links, WorkLinkRequestRepository ledger,
                            WorkVerificationRepository verifications, CurrentPositions positions,
                            Companies companies, CompanyMerges merges, Mailer mailer, Clock clock,
                            @Value("${docket.base-url:http://localhost:8080}") String baseUrl) {
        this.links = links;
        this.ledger = ledger;
        this.verifications = verifications;
        this.positions = positions;
        this.companies = companies;
        this.merges = merges;
        this.mailer = mailer;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    /**
     * Only a member with a current Position at the Company may ask (§16's currency,
     * applied at the door): without that, anyone who can receive mail anywhere could
     * graft their domain onto any Company — and §6.1's auto-merge would then let
     * them fold arbitrary companies together. The Position is a public, accountable
     * claim on the asker's own Profile; that is the cost of a link.
     */
    @Transactional
    Outcome request(long memberId, Company company, String address) {
        if (!positions.heldBy(memberId, company.id())) {
            return Outcome.NOT_ELIGIBLE;
        }
        String domain = domainOf(address);
        if (domain == null) {
            return Outcome.INVALID_ADDRESS;
        }
        Instant hourAgo = clock.instant().minus(Duration.ofHours(1));
        if (ledger.countByAddressAndCreatedAtAfter(address, hourAgo) >= MAX_PER_ADDRESS_PER_HOUR
                || ledger.countByMemberIdAndCreatedAtAfter(memberId, hourAgo) >= MAX_PER_MEMBER_PER_HOUR) {
            return Outcome.RATE_LIMITED;
        }
        Instant now = clock.instant();
        ledger.save(new WorkLinkRequest(memberId, address, now));
        String token = Tokens.generate();
        links.save(new WorkLink(Tokens.hash(token), memberId, company.id(), domain, address,
                now, now.plus(LINK_LIFETIME)));
        mailer.send(address, "Verify where you work", """
                Somebody with a Docket profile — hopefully you — asked to verify that they \
                can receive mail at this address, for the company page "%s".

                %s/verify/%s

                Following it records a dated fact on Docket: that you could receive mail at \
                this domain today. The link works once and expires in 30 minutes. If you \
                didn't ask for it, ignore this — nothing happens without the link."""
                .formatted(company.name(), baseUrl, token));
        return Outcome.SENT;
    }

    /**
     * Consume a link: store the dated fact (§16 — it never lapses), then let the
     * fact do its §6.1 work: if another Company already holds this domain, they were
     * the same Company all along, and they merge. Returns the id of the Company the
     * fact now belongs to.
     */
    @Transactional
    Optional<Long> consume(String token) {
        Instant now = clock.instant();
        return links.findByTokenHash(Tokens.hash(token))
                .filter(link -> link.usable(now))
                .map(link -> {
                    link.markUsed(now);
                    // The Company may itself have merged since the link was sent.
                    Company company = companies.findResolved(link.companyId()).orElseThrow();
                    verifications.save(new WorkVerification(link.memberId(), company.id(),
                            link.domain(), now));
                    return merges.mergeAnySharing(link.domain(), company.id());
                });
    }

    private static String domainOf(String address) {
        if (!PLAUSIBLE_EMAIL.matcher(address).matches()) {
            return null;
        }
        String domain = address.substring(address.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        return PLAUSIBLE_DOMAIN.matcher(domain).matches() ? domain : null;
    }
}
