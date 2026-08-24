package com.mbeebe.docket.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The trust gate (§6.2), derived at the point of asking (ADR-0002): a Member passes
 * it for a Company when they hold BOTH a current Position there (§16 — ending the
 * Position removes the right at once) AND a Work verification at one of the
 * Company's verified domains (a dated fact that never lapses). It gates the two
 * capabilities that can do harm: editing the page (here) and posting a Job under
 * the Company — the jobs board (#35) asks this exact question and nothing else.
 */
@Service
public class TrustGate {

    private final Companies companies;
    private final CurrentPositions positions;
    private final WorkVerificationRepository verifications;

    TrustGate(Companies companies, CurrentPositions positions,
              WorkVerificationRepository verifications) {
        this.companies = companies;
        this.positions = positions;
        this.verifications = verifications;
    }

    @Transactional(readOnly = true)
    public boolean passes(long memberId, long companyId) {
        Long resolved = companies.findResolved(companyId).map(Company::id).orElse(null);
        if (resolved == null || !positions.heldBy(memberId, resolved)) {
            return false;
        }
        List<String> verifiedDomains = verifications.domainsOf(resolved);
        return !verifiedDomains.isEmpty() && verifications.findByMemberId(memberId).stream()
                .map(WorkVerification::domain)
                .anyMatch(verifiedDomains::contains);
    }
}
