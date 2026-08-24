package com.mbeebe.docket.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The derived-capability core (ADR-0002): every gate computed from stored facts at
 * the point of asking. Today the facts are Completeness's inputs; moderation
 * withdrawals (#38) and the trust gate's work verifications (#34) compose in here
 * as they land, without any caller changing.
 */
@Service
public class CapabilityService {

    private final ProfileService profiles;

    CapabilityService(ProfileService profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public CapabilityAnswer may(long memberId, Capability capability) {
        return profiles.completenessOf(memberId).complete()
                ? CapabilityAnswer.YES
                : CapabilityAnswer.NOT_YET_EARNED;
    }
}
