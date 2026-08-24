package com.mbeebe.docket.jobs;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * The poster's queue (§6.4): every Application on one posting, oldest first —
 * a docket, worked in order. Deliberately absent: any way to contact anyone.
 * The poster's reply channel is the Application-scoped Thread (#36); nothing
 * here reaches a member who did not apply (§6.3).
 */
public record QueuePage(long postingId, String title, boolean open, String closesOn,
                        List<Row> rows) {

    public record Row(long applicationId, PersonCard applicant, String note, String appliedOn,
                      String stateLabel, boolean unresolved, List<PersonCard> mutuals) {
    }
}
