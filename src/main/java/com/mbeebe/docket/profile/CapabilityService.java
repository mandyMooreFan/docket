package com.mbeebe.docket.profile;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The derived-capability core (ADR-0002): every gate computed from stored facts at
 * the point of asking. The facts are Completeness's inputs and §10.3's moderation
 * actions; the trust gate's work verifications (#34) compose where the jobs board
 * asks, since they gate a Company rather than a Member.
 *
 * <p>The order of the two questions is the §10.3 requirement, not an implementation
 * detail. A Withdrawal is asked about first, so a Member whose Profile is also
 * incomplete is told the thing that is actually standing in their way: finishing the
 * Profile would not give a withdrawn Capability back, and saying "not yet earned"
 * would be a lie about their own account of exactly the kind §10.3 refuses.
 */
@Service
public class CapabilityService {

    private final ProfileService profiles;

    // Resolved per call rather than injected: moderation reads profiles, so eager
    // injection would close a cycle at startup. Absent contributor means nothing has
    // been withdrawn, which is also the honest answer before moderation exists.
    private final ObjectProvider<WithdrawnCapabilities> withdrawals;

    CapabilityService(ProfileService profiles, ObjectProvider<WithdrawnCapabilities> withdrawals) {
        this.profiles = profiles;
        this.withdrawals = withdrawals;
    }

    @Transactional(readOnly = true)
    public CapabilityAnswer may(long memberId, Capability capability) {
        if (withdrawn(memberId, capability)) {
            return CapabilityAnswer.WITHDRAWN;
        }
        if (capability.earnedByCompleteness() && !profiles.completenessOf(memberId).complete()) {
            return CapabilityAnswer.NOT_YET_EARNED;
        }
        return CapabilityAnswer.YES;
    }

    private boolean withdrawn(long memberId, Capability capability) {
        WithdrawnCapabilities contributor = withdrawals.getIfAvailable();
        return contributor != null && contributor.withdrawn(memberId, capability);
    }
}
