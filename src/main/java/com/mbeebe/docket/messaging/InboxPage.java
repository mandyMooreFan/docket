package com.mbeebe.docket.messaging;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * The inbox (§7.2): "a list of people", newest correspondence first. A Thread
 * closed to new Messages stays listed and readable — the history is yours
 * (§7.3, §11.1) — and nothing in a row says which Threads are closed or why.
 *
 * <p>Deliberately absent: any per-row count, dot or unread mark. §5.6 refuses
 * badges and dots outright, and §7.4's single exception is the one Unread count
 * on the Messages nav item — not a second one repeated down this list.
 */
public record InboxPage(List<Row> rows) {

    public record Row(long otherId, PersonCard person, String latest, String when) {
    }
}
