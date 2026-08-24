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
public record InboxPage(List<Row> rows, boolean hasConnections) {

    /**
     * §13.4's "Inbox, nothing writable" — the cold-start state its copy was
     * written for, and the state that copy is true in: its second sentence says
     * "No connections yet" in as many words.
     */
    public boolean noGraphYet() {
        return rows.isEmpty() && !hasConnections;
    }

    /**
     * Also an empty inbox, and NOT §13.4's: this member has connections and simply
     * nobody has written yet. Telling them "no connections yet — your inbox is
     * waiting on the graph, not on you" would be a plain untruth, and §13.4's copy
     * is editable in tone, not in honesty.
     */
    public boolean nothingSaidYet() {
        return rows.isEmpty() && hasConnections;
    }

    public record Row(long otherId, PersonCard person, String latest, String when) {
    }
}
