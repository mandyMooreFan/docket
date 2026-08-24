package com.mbeebe.docket.jobs;

import com.mbeebe.docket.identity.Mailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * §6.5's transactional mail — application received; application closed — on
 * the one shared Mailer port (§14.2). The deliberate exception to the
 * product's no-email rule: postings are time-limited and an employed seeker
 * cannot check daily. Nothing else here ever mails anyone.
 */
@Component
class JobMails {

    private final Mailer mailer;
    private final String baseUrl;

    JobMails(Mailer mailer, @Value("${docket.base-url:http://localhost:8080}") String baseUrl) {
        this.mailer = mailer;
        this.baseUrl = baseUrl;
    }

    void received(String to, JobPosting posting, String companyName) {
        mailer.send(to, "Application received — " + posting.title(), """
                We received your application to “%s” at %s. Your profile is the \
                application — that's what the poster sees, with your note if you left one.

                %s/jobs/%d

                You can always see where it stands: %s/applications. Every application \
                gets an outcome — advanced, not selected, or closed without response — \
                by the time the posting is done."""
                .formatted(posting.title(), companyName, baseUrl, posting.id(), baseUrl));
    }

    void notSelected(String to, JobPosting posting, String companyName) {
        mailer.send(to, "Your application closed — " + posting.title(), """
                Your application to “%s” at %s was not selected. That's its \
                outcome, recorded where it always is:

                %s/applications"""
                .formatted(posting.title(), companyName, baseUrl));
    }

    void closedWithoutResponse(String to, JobPosting posting, String companyName) {
        mailer.send(to, "Your application closed — " + posting.title(), """
                The posting “%s” at %s closed without a response to your \
                application. That's an outcome too, and you're owed hearing it:

                %s/applications"""
                .formatted(posting.title(), companyName, baseUrl));
    }
}
