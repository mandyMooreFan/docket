package com.mbeebe.docket.invites;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The Invite as a reportable object (§10.2). An Invite is a way to email a
 * stranger, so it enters the report queue like a Post or a Message does — and the
 * report will usually arrive by §10.2's published contact address, because the
 * person complaining about an Invite is by definition not a Member and has no
 * in-product Report button to press.
 *
 * <p>Moderation (#38) owns the queue and the ladder. This module owns the target:
 * a stable id that outlives the mail, and the facts a report would name — who
 * sent it, to which address, with which words, when. Everything a moderator needs
 * to match a forwarded complaint to a row and act on the sender is here, and
 * nothing else is, because nothing else exists (see {@link Invite}).
 *
 * <p>The rung this composes with is §10.3's second: withdraw the Capability that
 * was abused. {@code Capability.INVITE} is that Capability, and because
 * {@code CapabilityService} derives every answer at the point of asking, a
 * withdrawal takes hold on the sender's very next attempt with nothing here to
 * change — and also on any Invite of theirs still waiting to land, since the
 * request materialises through the same gate (see
 * {@code graph.ConnectionRequests}).
 */
@Service
public class Invites {

    /** One Invite as a report names it (§10.2). */
    public record Reportable(long id, long senderId, String recipientEmail, String note,
                             Instant sentAt) {
    }

    private final InviteRepository invites;

    Invites(InviteRepository invites) {
        this.invites = invites;
    }

    @Transactional(readOnly = true)
    public Optional<Reportable> reportable(long inviteId) {
        return invites.findById(inviteId).map(Invites::facts);
    }

    /** Every Invite that reached one address — how a forwarded complaint finds its row. */
    @Transactional(readOnly = true)
    public List<Reportable> sentTo(String email) {
        return invites.findByEmailIgnoreCaseOrderBySentAtDesc(email).stream()
                .map(Invites::facts)
                .toList();
    }

    /** Every Invite one Member sent — the pattern a report about a sender is judged on. */
    @Transactional(readOnly = true)
    public List<Reportable> sentBy(long senderId) {
        return invites.findBySenderIdOrderBySentAtDesc(senderId).stream()
                .map(Invites::facts)
                .toList();
    }

    private static Reportable facts(Invite invite) {
        return new Reportable(invite.id(), invite.senderId(), invite.email(),
                invite.note(), invite.sentAt());
    }
}
