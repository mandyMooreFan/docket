package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Mailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The statutory surfaces the product carries in words (§15.3): the acknowledgement of a
 * Report (DSA Art. 16), the statement of reasons for a decision (Art. 17), and the
 * disclosures §10.5 owes both parties to an auto-hide.
 *
 * <p>v1 does not target the EU (§15.4), so these fields apply "where they apply" — they
 * are carried anyway, because carrying them costs a paragraph and not carrying them
 * would mean deciding, per message, whether this is one of the times it matters.
 *
 * <p>Art. 17's five required contents are each a line below, and one of them collapses
 * to nothing: category (1), "whether the decision is removal, disabling, demotion or
 * restriction of visibility", has only one possible answer here, because §10.3 refused
 * demotion and visibility limiting outright. The mail says removal or it says nothing
 * happened.
 */
@Component
class ModerationMails {

    private final Mailer mailer;
    private final String baseUrl;

    ModerationMails(Mailer mailer, @Value("${docket.base-url:http://localhost:8080}") String baseUrl) {
        this.mailer = mailer;
        this.baseUrl = baseUrl;
    }

    /**
     * Art. 16's confirmation of receipt, sent in the same transaction as the Report so
     * that a Report which exists has always been acknowledged.
     */
    void acknowledgeReport(String to, Report report) {
        mailer.send(to, "We have your report", """
                We received your report about %s, in the category “%s”.

                What happens now: one person reviews reports, in the order they \
                arrive, and usually within a few days. That is the honest expectation, \
                not a service level — Docket is run by one person in one timezone.

                You will get a second message when it has been decided, with the \
                reasons and with what you can do if you disagree.

                You reported:

                %s""".formatted(describe(report.targetKind()), report.category().label(),
                report.account()));
    }

    /**
     * Art. 17's statement of reasons, owed to the Member whose content or account was
     * restricted, at the latest from the moment the restriction takes effect.
     */
    void statementOfReasons(String to, String whatWasDone, String reason,
                            boolean followedAReport, Optional<Long> appealableActionId) {
        String redress = appealableActionId
                .map(id -> """
                        If you think this is wrong, you can appeal once: \
                        %s/appeals/%d

                        An appeal is exactly what it sounds like and nothing more — \
                        the same person looking again, with whatever new information \
                        you give them. Docket is not going to pretend it has an \
                        independent panel.""".formatted(baseUrl, id))
                .orElse("""
                        There is nothing to appeal against here, because nothing was \
                        restricted.""");

        mailer.send(to, "A decision about your account", """
                %s

                Why: %s

                This followed %s. No automated system made this decision, and none \
                could have: everything on Docket is reported by a person and judged \
                by a person (§10.4). The basis is the member conduct policy, which \
                lists what a member may not do and treats everything it does not list \
                as not an offence — you can read it at %s/conduct.

                %s""".formatted(whatWasDone, reason,
                followedAReport ? "a report from another member"
                        : "a review started by the owner, not a report",
                baseUrl, redress));
    }

    /**
     * §10.5: an auto-hide is "disclosed to both parties". The uploader is told their
     * content is hidden and why, in terms that do not accuse them — the hold is
     * pre-decision and implies no finding.
     */
    void holdDisclosedToUploader(String to, String href) {
        mailer.send(to, "Something you posted is hidden while it is checked", """
                Someone has reported an item you posted under the intimate image \
                route, which the law requires us to treat differently from an \
                ordinary report.

                What that means: the item is hidden now, before anyone has looked at \
                it. That is not a finding against you, and it is not a mark on your \
                account. It is a hold, it is reversible, and a person will look at it \
                and either restore it or remove it.

                The item: %s

                We are telling you because you are entitled to know that something of \
                yours is hidden and why. If it is restored you will hear again.""".formatted(href));
    }

    /** The other party to the same hold: the person who reported it, told it took effect. */
    void holdDisclosedToReporter(String to) {
        mailer.send(to, "The content you reported is hidden", """
                The content you reported is hidden now. It happened when your report \
                arrived, not after a review — that is how this route works, so that \
                nothing waits on someone being awake.

                A person will look at it and either confirm the removal or restore it. \
                Either way you will hear the outcome.

                If you gave us a location we could not make sense of, we may write back \
                to ask. You do not need an account for any of this.""");
    }

    /** DUAA s.164A: acknowledgement of a data-protection complaint, owed within 30 days. */
    void acknowledgeComplaint(String to) {
        mailer.send(to, "We have your data protection complaint", """
                We received your complaint about how Docket handles personal data.

                The law gives us 30 days to acknowledge it — this message is that \
                acknowledgement, sent now rather than on the thirtieth day — and \
                requires us to respond. One person handles these.

                This route is separate from reporting content, on purpose. If you also \
                want to report something on the site, that is a different form and it \
                is at %s/report.

                If you are unhappy with how we handle this, you can complain to the \
                Information Commissioner's Office at ico.org.uk.""".formatted(baseUrl));
    }

    private String describe(TargetKind kind) {
        return switch (kind) {
            case PROFILE -> "a profile";
            case POST -> "a post";
            case REPLY -> "a reply";
            case MESSAGE -> "a message";
            case JOB_POSTING -> "a job posting";
            case COMPANY -> "a company page";
        };
    }
}
