package com.mbeebe.docket.jobs;

import com.mbeebe.docket.leaving.Departure;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The board at Termination (§11.2).
 *
 * <p><strong>Applications and saved searches go.</strong> An Application was one
 * click offering a Profile that no longer exists (§6.3); there is nothing left for
 * a poster to look at, and leaving the row would put a dead applicant in a live
 * queue. A saved search is a standing instruction to send email to an address that
 * has gone.
 *
 * <p><strong>Postings the member wrote are closed, not deleted</strong>, and the
 * difference matters. §6.4's guarantee is owed to the people who applied — every
 * Application gets an Outcome or a recorded close — and deleting the posting would
 * take their Applications with it, which is the leaver reaching into other
 * members' records. Closing is what "unpublished" means in the board's own
 * vocabulary: it leaves the board and stops accepting Applications, exactly as it
 * would have when its window ran out, and everyone who applied keeps their row and
 * their answer.
 *
 * <p>Accepted cost: an open posting can vanish from the board mid-window because
 * its author left, and the sweep's separate close-without-response record is not
 * written here — {@code PostingCloser} still owns that, and will find these on its
 * next pass over anything past its edge.
 */
@Component
@Order(50)
class JobsDeparture implements Departure {

    private final JobApplicationRepository applications;
    private final JobPostingRepository postings;
    private final JobSearchRepository searches;
    private final Clock clock;

    JobsDeparture(JobApplicationRepository applications, JobPostingRepository postings,
                  JobSearchRepository searches, Clock clock) {
        this.applications = applications;
        this.postings = postings;
        this.searches = searches;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void memberLeaving(long memberId) {
        applications.findByApplicantIdOrderByAppliedAtDescIdDesc(memberId)
                .forEach(applications::delete);
        searches.findByMemberIdAndStoppedAtIsNullOrderByCreatedAt(memberId)
                .forEach(search -> search.stop(clock.instant()));
        postings.findByPosterIdOrderByPostedAtDesc(memberId)
                .forEach(posting -> posting.markClosed(clock.instant()));
    }
}
