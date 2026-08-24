package com.mbeebe.docket.jobs;

import com.mbeebe.docket.messaging.ApplicationChannel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The board's answer to {@link ApplicationChannel} (§7.1, ADR-0001): the one
 * question messaging asks of the jobs side. The window itself is documented on
 * {@link JobApplicationRepository#openChannelsBetween} — it belongs here,
 * because whether an Application is still running is a fact about the board,
 * not something messaging should be reconstructing.
 *
 * <p>Scoped to the pair, so the §6.3 rule survives intact: nothing lets a
 * poster contact members who did not apply.
 */
@Component
class ChannelForMessaging implements ApplicationChannel {

    private final JobApplicationRepository applications;
    private final Clock clock;

    ChannelForMessaging(JobApplicationRepository applications, Clock clock) {
        this.applications = applications;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean openBetween(long memberA, long memberB) {
        return memberA != memberB && applications.openChannelsBetween(memberA, memberB,
                JobApplication.Outcome.ADVANCED, clock.instant()) > 0;
    }
}
