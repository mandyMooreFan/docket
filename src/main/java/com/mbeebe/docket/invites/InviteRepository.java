package com.mbeebe.docket.invites;

import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface InviteRepository extends Repository<Invite, Long> {

    Invite save(Invite invite);

    Optional<Invite> findById(Long id);

    /**
     * Case-insensitive throughout, and for one reason: an address the sender typed
     * as "Bob@" and the joiner typed as "bob@" is one address to a postmaster, so
     * it must be one address to the ledger too, or the limits and the landing both
     * come apart on a shift key.
     */
    long countBySenderIdAndSentAtAfter(Long senderId, Instant cutoff);

    long countByEmailIgnoreCaseAndSentAtAfter(String email, Instant cutoff);

    List<Invite> findByEmailIgnoreCaseAndLandedAtIsNull(String email);

    List<Invite> findBySenderIdOrderBySentAtDesc(Long senderId);

    List<Invite> findByEmailIgnoreCaseOrderBySentAtDesc(String email);
}
