package com.mbeebe.docket.invites;

import com.mbeebe.docket.identity.Mailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * §13.3's mail, on the one shared Mailer port (§14.2) — the third outbound
 * source after magic links and work-address verification, and the one that
 * scales with enthusiasm against Resend's 100/day free tier.
 *
 * <p>What this mail deliberately does not do: carry a token. §13.3's "optional,
 * never a gate" is only a promise if the link goes to the same open /join
 * everybody else uses. There is nothing here to redeem, so there is nothing that
 * could make an invited signup differ from an ordinary one — the waiting request
 * is found by the address at the far end, not by anything the invitee clicks.
 *
 * <p>What it also does not do: promise an outcome. §9.2 can refuse the waiting
 * request, so the mail describes the inviter's intent and never guarantees what
 * the invitee will find. Nor does it name the inviter's email address, or link
 * to their Profile — the Dial may put that page out of a logged-out reach (§4.1).
 */
@Component
class InviteMails {

    private final Mailer mailer;
    private final String baseUrl;

    InviteMails(Mailer mailer, @Value("${docket.base-url:http://localhost:8080}") String baseUrl) {
        this.mailer = mailer;
        this.baseUrl = baseUrl;
    }

    void invite(String to, String senderName, String note) {
        String quoted = note.isBlank() ? "" : note.lines()
                .map(line -> "    " + line)
                .reduce((one, two) -> one + "\n" + two)
                .orElse("") + "\n\n";
        mailer.send(to, senderName + " invited you to Docket", """
                %s invited you to Docket and would like to connect with you there.

                %sDocket is a professional network built around one page: a permanent, \
                well-designed record of your working life, at a URL you control. Anyone \
                can join, and this invite is not a key — it opens no door you \
                couldn't open yourself:

                %s/join

                Joining is completely ordinary; this gates nothing and \
                changes nothing about it. If you would rather not, do nothing at all. \
                Nobody is told, and no reminder follows this."""
                .formatted(senderName, quoted, baseUrl));
    }
}
