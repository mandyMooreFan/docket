package com.mbeebe.docket.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/** What other modules may ask identity about a Member; the repository stays internal. */
@Service
public class Members {

    private final MemberRepository repository;
    private final MemberSessionRepository sessions;
    private final MagicLinkRepository links;
    private final Clock clock;

    Members(MemberRepository repository, MemberSessionRepository sessions,
            MagicLinkRepository links, Clock clock) {
        this.repository = repository;
        this.sessions = sessions;
        this.links = links;
        this.clock = clock;
    }

    public Optional<Member> find(long id) {
        return repository.findById(id);
    }

    /**
     * Identity's half of Termination (§11.2): the account ends here.
     *
     * <p>Called last, after every module has taken its own rows out through
     * {@code leaving.Departure}, because until then those modules still need to
     * find the Member they are erasing. Three things happen and nothing else:
     * every session is destroyed (a session outliving its account would be the
     * only way back in), every unconsumed magic link for the old address is
     * destroyed with it (§3.1's links are live credentials), and the Member row
     * becomes the tombstone {@link Member#terminate} describes.
     *
     * <p>Deliberately absent: any attempt to delete the row. See V13 — the
     * correspondence that survives is anchored to this id.
     */
    @Transactional
    public void terminate(long memberId, String reason) {
        repository.findById(memberId).ifPresent(member -> {
            sessions.deleteByMember(member);
            links.deleteByEmail(member.email());
            member.terminate(reason, clock.instant());
        });
    }
}
