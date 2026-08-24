package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.identity.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * The end of a Member (CONTEXT.md), reached two ways and built once.
 *
 * <p>This is the seam agreed with §11.2's effort (#39): §10.3's fourth rung and the
 * Member's own front door need the same primitive, and a primitive built twice is a
 * primitive that disagrees with itself. What lives here is only what the ladder needs —
 * the Member ends, their sessions end, and the record of it is written. What lives
 * behind it, for #39 to fill in, is everything §11.2 asks of the member-facing flow:
 * the export offered first, the former Member's side of each Thread staying attributed,
 * the Recommendations they wrote staying published, and the backups beyond-use copy.
 *
 * <p>Nothing is deleted. The Member row and every reference to it survive, because
 * §7.3 and §11.1 both say the other person's record is not the terminated Member's to
 * destroy — the references across the product deliberately do not cascade so that this
 * stays true by construction rather than by care.
 */
@Service
public class Terminations {

    private final MemberTerminationRepository terminations;
    private final Members members;
    private final SessionService sessions;
    private final Clock clock;

    Terminations(MemberTerminationRepository terminations, Members members,
                 SessionService sessions, Clock clock) {
        this.terminations = terminations;
        this.members = members;
        this.sessions = sessions;
        this.clock = clock;
    }

    /**
     * §11.2's front door: the Member's own decision to leave. Public because #39 owns
     * the flow that leads here and lives in another module.
     */
    @Transactional
    public void terminateAtMemberRequest(long memberId) {
        terminate(memberId, MemberTermination.Cause.MEMBER, "");
    }

    /** §10.3's fourth rung. The reason is the moderator's, and it is always recorded. */
    @Transactional
    void terminateByModeration(long memberId, String reason) {
        terminate(memberId, MemberTermination.Cause.MODERATION, reason);
    }

    private void terminate(long memberId, MemberTermination.Cause cause, String reason) {
        if (terminations.existsByMemberId(memberId)) {
            return;
        }
        terminations.save(new MemberTermination(memberId, cause, reason, clock.instant()));
        Optional<Member> member = members.find(memberId);
        member.ifPresent(sessions::endAllSessions);
    }
}
