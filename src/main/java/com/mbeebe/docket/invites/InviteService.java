package com.mbeebe.docket.invites;

import com.mbeebe.docket.identity.Addresses;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Sending an Invite (§13.3).
 *
 * <p><strong>No quota.</strong> §13.3 rules one out in as many words — a quota
 * throttles exactly the members doing the seeding, which is the one behaviour
 * cold start (§13.2) depends on. There is no lifetime cap here and no count of
 * how many people a member has ever invited.
 *
 * <p><strong>Rate limits instead, per sender and per address</strong>, in
 * identity's hourly {@code link_request} shape and inherited from §3.3 because an
 * Invite is a way to email a stranger. They bound the burst, not the total: a
 * member who keeps inviting keeps being able to.
 *
 * <p><strong>No membership oracle (§8.3).</strong> An Invite to an address that
 * already belongs to a Member is accepted, ledgered and answered exactly like any
 * other; the only difference is that nothing is posted, and that difference is
 * invisible from the sender's side of the transaction. It is the mirror of
 * identity's sign-in rule, where an unknown address gets no mail and the same
 * screen. Nothing else happens either: turning it into a Connection request on
 * the spot would route around §4.2's own affordance and leak the same fact.
 */
@Service
class InviteService {

    /** §3.3's hourly shape, per sender: enough to seed a room, not to run a campaign. */
    static final int MAX_PER_SENDER_PER_HOUR = 10;

    /** And per address, matching identity's link limit: nobody's inbox is a target. */
    static final int MAX_PER_ADDRESS_PER_HOUR = 3;

    /** A note, not a message (§4.2's shape): the Invite is not a messaging channel. */
    static final int MAX_NOTE = 1_000;

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK).withZone(ZoneId.systemDefault());

    enum Outcome { SENT, INVALID_EMAIL, BLOCKED_DOMAIN, RATE_LIMITED, NOTE_TOO_LONG }

    private final InviteRepository invites;
    private final InviteMails mails;
    private final Addresses addresses;
    private final CapabilityService capabilities;
    private final ProfileService profiles;
    private final Clock clock;

    InviteService(InviteRepository invites, InviteMails mails, Addresses addresses,
                  CapabilityService capabilities, ProfileService profiles, Clock clock) {
        this.invites = invites;
        this.mails = mails;
        this.addresses = addresses;
        this.capabilities = capabilities;
        this.profiles = profiles;
        this.clock = clock;
    }

    boolean mayInvite(long memberId) {
        return capabilities.may(memberId, Capability.INVITE) == CapabilityAnswer.YES;
    }

    @Transactional
    Outcome send(Member sender, String rawEmail, String rawNote) {
        String email = rawEmail == null ? "" : rawEmail.strip();
        String note = rawNote == null ? "" : rawNote.strip();
        if (note.length() > MAX_NOTE) {
            return Outcome.NOTE_TOO_LONG;
        }
        Addresses.Verdict verdict = addresses.check(email);
        if (verdict == Addresses.Verdict.NOT_AN_ADDRESS) {
            return Outcome.INVALID_EMAIL;
        }
        if (verdict == Addresses.Verdict.BLOCKED_DOMAIN) {
            return Outcome.BLOCKED_DOMAIN;
        }
        Instant hourAgo = clock.instant().minus(Duration.ofHours(1));
        if (invites.countBySenderIdAndSentAtAfter(sender.id(), hourAgo) >= MAX_PER_SENDER_PER_HOUR
                || invites.countByEmailIgnoreCaseAndSentAtAfter(email, hourAgo)
                        >= MAX_PER_ADDRESS_PER_HOUR) {
            return Outcome.RATE_LIMITED;
        }
        // The ledger row lands whichever verdict came back, so an address with an
        // account consumes the same budget an address without one does (§8.3).
        invites.save(new Invite(sender.id(), email, note, clock.instant()));
        if (verdict == Addresses.Verdict.STRANGER) {
            mails.invite(email, profiles.cardFor(sender.id()).displayName(), note);
        }
        return Outcome.SENT;
    }

    @Transactional(readOnly = true)
    InvitePage pageFor(long memberId, boolean justSent, String error) {
        List<InvitePage.Sent> sent = invites.findBySenderIdOrderBySentAtDesc(memberId).stream()
                .map(invite -> new InvitePage.Sent(invite.email(), DAY.format(invite.sentAt())))
                .toList();
        return new InvitePage(mayInvite(memberId), justSent, error, sent);
    }

    static String messageFor(Outcome outcome) {
        return switch (outcome) {
            case INVALID_EMAIL -> "That doesn't look like an email address.";
            case BLOCKED_DOMAIN -> "That provider's inboxes are public — anyone could open "
                    + "an account with that address. Docket blocks them as a security rule; "
                    + "private aliases (Hide My Email, SimpleLogin and the like) work fine.";
            case RATE_LIMITED -> "Too many invites for now. Wait a while and send more — "
                    + "this is a rate limit, not a quota, and there is no cap on how many "
                    + "people you can invite.";
            case NOTE_TOO_LONG -> "That note is longer than an Invite carries. "
                    + "Keep it to a line or two.";
            case SENT -> null;
        };
    }
}
