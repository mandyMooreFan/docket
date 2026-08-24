package com.mbeebe.docket.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * §6.1's identity rule, enacted: entities sharing a verified domain are the same
 * Company, so they merge. Every merge is recorded with every row it moved (§10.5:
 * audited, reversible); the absorbed Company's row survives with a pointer, so its
 * URL redirects and a reversal is mechanical. Manual merges and reversal act
 * through the same facts and arrive with moderation (#38) — no UI exists here.
 */
@Service
public class CompanyMerges {

    private final CompanyRepository companyRepository;
    private final WorkVerificationRepository verifications;
    private final CompanyEditRepository edits;
    private final CurrentPositions positions;
    private final CompanyMergeRepository mergeRepository;
    private final CompanyMergeItemRepository items;
    private final Clock clock;

    CompanyMerges(CompanyRepository companyRepository, WorkVerificationRepository verifications,
                  CompanyEditRepository edits, CurrentPositions positions,
                  CompanyMergeRepository mergeRepository, CompanyMergeItemRepository items,
                  Clock clock) {
        this.companyRepository = companyRepository;
        this.verifications = verifications;
        this.edits = edits;
        this.positions = positions;
        this.mergeRepository = mergeRepository;
        this.items = items;
        this.clock = clock;
    }

    /**
     * The auto-merge check, run when a Work verification lands: fold every other
     * Company holding this domain into one. The oldest entity survives — its
     * identity is the senior claim; the newer name was a fork that never should
     * have existed. Returns the surviving Company's id.
     */
    @Transactional
    public long mergeAnySharing(String domain, long companyId) {
        List<Long> holders = verifications.companyIdsHolding(domain);
        long survivorId = holders.stream().mapToLong(Long::longValue).min().orElse(companyId);
        for (Long holder : holders) {
            if (holder != survivorId) {
                merge(holder, survivorId, CompanyMerge.Cause.SHARED_DOMAIN, null);
            }
        }
        return survivorId;
    }

    /** One merge, recorded whole: the moved rows are the audit trail (§10.5). */
    @Transactional
    public void merge(long absorbedId, long survivorId, CompanyMerge.Cause cause,
                      Long actorMemberId) {
        Company absorbed = companyRepository.findById(absorbedId).orElseThrow();
        if (absorbed.merged()) {
            return;
        }
        CompanyMerge merge = mergeRepository.save(new CompanyMerge(absorbedId, survivorId,
                cause, actorMemberId, clock.instant()));
        for (Long positionId : positions.repointAll(absorbedId, survivorId)) {
            items.save(new CompanyMergeItem(merge.id(), CompanyMergeItem.Kind.POSITION, positionId));
        }
        for (WorkVerification verification : verifications.findByCompanyId(absorbedId)) {
            verification.repointTo(survivorId);
            items.save(new CompanyMergeItem(merge.id(),
                    CompanyMergeItem.Kind.WORK_VERIFICATION, verification.id()));
        }
        for (CompanyEdit edit : edits.findByCompanyIdOrderByIdAsc(absorbedId)) {
            edit.repointTo(survivorId);
            items.save(new CompanyMergeItem(merge.id(),
                    CompanyMergeItem.Kind.COMPANY_EDIT, edit.id()));
        }
        absorbed.markMergedInto(survivorId);
    }
}
