package com.mbeebe.docket.jobs;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * The poster's queue (§6.4): every Application on one posting, oldest first —
 * a docket, worked in order. The only way to contact anyone from here is
 * {@code mayMessage}: the Application-scoped Thread (§7.1, #36), open exactly
 * while this Application is still running. Nothing here reaches a member who
 * did not apply (§6.3).
 */
public record QueuePage(long postingId, String title, boolean open, String closesOn,
                        List<Row> rows) {

    public record Row(long applicationId, PersonCard applicant, String note, String appliedOn,
                      String stateLabel, boolean unresolved, List<PersonCard> mutuals,
                      boolean mayMessage) {
    }
}
