package com.mbeebe.docket.moderation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The DUAA s.164A data-protection complaints route (§11.3, §15.3).
 *
 * <p>Its own form and its own service, because the statute makes it its own thing. It
 * is not a Report — §10.2's route is about content breaking the conduct policy, and
 * this is about how Docket handles personal data — and it is not the intimate-image
 * route either. §11.3 records that there is no small-operator, non-profit or
 * open-source exemption from any of it, and that being free is expressly irrelevant.
 *
 * <p>No account required, for the same reason §10.5's route needs none: the person with
 * a complaint about data handling may be someone whose data was handled without ever
 * having been a Member.
 *
 * <p>The 30-day acknowledgement duty is met by acknowledging on receipt. There is no
 * timer and no sweep, which is the same trick §10.5 plays with its 48 hours: a duty
 * discharged in the same transaction cannot be a duty that lapses.
 */
@Service
class ComplaintService {

    static final int MAX_PER_ADDRESS_PER_HOUR = 3;
    static final int MAX_PER_IP_PER_HOUR = 10;

    private final DataProtectionComplaintRepository complaints;
    private final PublicFormRequestRepository ledger;
    private final ModerationMails mails;
    private final Clock clock;

    ComplaintService(DataProtectionComplaintRepository complaints,
                     PublicFormRequestRepository ledger,
                     ModerationMails mails, Clock clock) {
        this.complaints = complaints;
        this.ledger = ledger;
        this.mails = mails;
        this.clock = clock;
    }

    enum Outcome {
        ACKNOWLEDGED,
        INCOMPLETE,
        RATE_LIMITED
    }

    @Transactional
    Outcome complain(String contact, String account, String requestIp) {
        String address = contact == null ? "" : contact.strip();
        String written = account == null ? "" : account.strip();
        if (address.isBlank() || written.isBlank()) {
            return Outcome.INCOMPLETE;
        }
        Instant now = clock.instant();
        Instant hourAgo = now.minus(Duration.ofHours(1));
        if (ledger.countByFormAndContactAndCreatedAtAfter(
                PublicFormRequest.Form.DATA_PROTECTION, address, hourAgo) >= MAX_PER_ADDRESS_PER_HOUR
                || ledger.countByFormAndRequestIpAndCreatedAtAfter(
                PublicFormRequest.Form.DATA_PROTECTION, requestIp, hourAgo) >= MAX_PER_IP_PER_HOUR) {
            return Outcome.RATE_LIMITED;
        }
        ledger.save(new PublicFormRequest(
                PublicFormRequest.Form.DATA_PROTECTION, address, requestIp, now));

        complaints.save(new DataProtectionComplaint(address, written, requestIp, now));
        mails.acknowledgeComplaint(address);
        return Outcome.ACKNOWLEDGED;
    }

    @Transactional(readOnly = true)
    List<ComplaintView> open() {
        return complaints.findByRespondedAtIsNullOrderByCreatedAtAscIdAsc().stream()
                .map(complaint -> new ComplaintView(
                        complaint.id(), complaint.contact(), complaint.account()))
                .toList();
    }

    @Transactional
    void respond(long complaintId, String response) {
        complaints.findById(complaintId)
                .orElseThrow(() -> new ModerationService.Refused("There is no such complaint."))
                .respond(response, clock.instant());
    }
}
