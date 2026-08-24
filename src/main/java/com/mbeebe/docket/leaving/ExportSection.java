package com.mbeebe.docket.leaving;

import java.util.List;

/**
 * One titled group of stored facts in the archive: the member's Positions, their
 * Posts, their Threads.
 *
 * <p>{@code kind} is the one thing here that is not presentation. A CORRESPONDENCE
 * section contains another living person's words as well as the member's own, and
 * §11.1 attaches a condition to that — the plain-language note about what those
 * words are and are not for. So the archive keeps correspondence in its own
 * document, opened by that note, rather than folding it into the run of the
 * member's own writing where the note would be a paragraph somebody scrolls past.
 * The distinction is the spec's, not the layout's.
 *
 * @param note a sentence shown above the records; empty when there is nothing to say
 */
public record ExportSection(String key, String title, String note, Kind kind,
                            List<ExportRecord> records) {

    /**
     * §11.1's note, and the reason this factory does not take one: the note is not
     * a caption a contributor chooses, it is the purpose condition WP242 attaches
     * to handing over a two-party record. Making it structural means a section
     * carrying somebody else's words cannot be built without it.
     */
    public static final String CORRESPONDENCE_NOTE =
            "Other people's words are in here. They are yours to keep, not to reuse.";

    /** Whose words are in here — the only thing the archive's shape turns on. */
    public enum Kind {

        /** The member's own stored facts and their own writing. */
        OWN,

        /** A two-party record: also somebody else's words (§11.1, WP242). */
        CORRESPONDENCE
    }

    public static ExportSection of(String key, String title, List<ExportRecord> records) {
        return new ExportSection(key, title, "", Kind.OWN, records);
    }

    public static ExportSection of(String key, String title, String note,
                                   List<ExportRecord> records) {
        return new ExportSection(key, title, note, Kind.OWN, records);
    }

    public static ExportSection correspondence(String key, String title,
                                               List<ExportRecord> records) {
        return new ExportSection(key, title, CORRESPONDENCE_NOTE, Kind.CORRESPONDENCE, records);
    }

    public boolean empty() {
        return records.isEmpty();
    }

    ExportSection with(List<ExportRecord> resolved) {
        return new ExportSection(key, title, note, kind, resolved);
    }
}
