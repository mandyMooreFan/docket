package com.mbeebe.docket.moderation;

import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.WithdrawnCapabilities;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Where a Member stands against §10.3's ladder, derived at the ask from the action
 * rows (ADR-0002). Nothing here is stored; "suspended" is a query, not a column, which
 * is what lets a Withdrawal with a stated period stop biting at its end without a sweep
 * running to notice.
 *
 * <p>The three rungs collapse into one answer for §3.2's gates, and the collapse is the
 * spec's, not a shortcut: a Suspension is read-only, so every write Capability is gone;
 * a Termination is the end of the Member, so they are gone too. Implementing {@link
 * WithdrawnCapabilities} is how that reaches the eight existing gates without any of
 * them learning what a rung is.
 */
@Service
class MemberStanding implements WithdrawnCapabilities {

    private final ModerationActionRepository actions;
    private final MemberTerminationRepository terminations;
    private final Clock clock;

    MemberStanding(ModerationActionRepository actions,
                   MemberTerminationRepository terminations,
                   Clock clock) {
        this.actions = actions;
        this.terminations = terminations;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean withdrawn(long memberId, Capability capability) {
        if (terminations.existsByMemberId(memberId)) {
            return true;
        }
        Instant now = clock.instant();
        return live(memberId, now).anyMatch(action -> switch (action.kind()) {
            case SUSPENSION, TERMINATION -> true;
            case WITHDRAWAL -> action.capability() == capability;
            case REMOVAL -> false;
        });
    }

    /** Read-only, but still able to sign in (CONTEXT.md) — the rung, not the whole account. */
    @Transactional(readOnly = true)
    boolean suspended(long memberId) {
        return live(memberId, clock.instant())
                .anyMatch(action -> action.kind() == ModerationAction.Kind.SUSPENSION);
    }

    @Transactional(readOnly = true)
    boolean terminated(long memberId) {
        return terminations.existsByMemberId(memberId);
    }

    /**
     * What the Member is told, in the §10.3 shape: every rung standing against them
     * right now, with its reason and its end. This is the whole of "the member is told
     * which they are in" — a withdrawn Capability reads differently from one never
     * earned because this answer exists and the never-earned case has none.
     */
    @Transactional(readOnly = true)
    List<StandingNotice> noticesFor(long memberId) {
        Instant now = clock.instant();
        return live(memberId, now)
                .filter(action -> action.kind() != ModerationAction.Kind.REMOVAL)
                .map(action -> new StandingNotice(
                        action.id(),
                        action.kind(),
                        action.capability(),
                        action.reason(),
                        Optional.ofNullable(action.until()),
                        action.actedAt()))
                .toList();
    }

    private java.util.stream.Stream<ModerationAction> live(long memberId, Instant now) {
        return actions.findByMemberIdAndReversedAtIsNull(memberId).stream()
                .filter(action -> action.inForceAt(now));
    }

    /** One standing rung, in the words the Member reads. */
    record StandingNotice(long actionId,
                          ModerationAction.Kind kind,
                          Capability capability,
                          String reason,
                          Optional<Instant> until,
                          Instant actedAt) {

        /** Indefinite is a real answer and is said plainly, never disguised as a long date. */
        boolean indefinite() {
            return until.isEmpty();
        }
    }
}
