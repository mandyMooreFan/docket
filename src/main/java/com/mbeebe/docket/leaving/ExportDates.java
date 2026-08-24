package com.mbeebe.docket.leaving;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * How dates read inside the archive (§11.1: documents, not a developer artefact).
 *
 * <p>Shared so that seven contributors cannot each pick a different one — an
 * archive whose sections disagree about what a date looks like is exactly the
 * developer artefact §11.1 is refusing. Long month names rather than the product's
 * clipped {@code d MMM uuuu}: this is read once, years later, possibly by a
 * solicitor, and there is no column to fit.
 */
public final class ExportDates {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK);
    private static final DateTimeFormatter MOMENT =
            DateTimeFormatter.ofPattern("d MMMM uuuu, HH:mm", Locale.UK);

    private ExportDates() {
    }

    /** A day, for facts whose time of day carries nothing. */
    public static String on(Instant instant, Clock clock) {
        return instant == null ? "" : DAY.format(instant.atZone(clock.getZone()));
    }

    /** A day and a time, for anything whose ordering a reader may need to follow. */
    public static String at(Instant instant, Clock clock) {
        return instant == null ? "" : MOMENT.format(instant.atZone(clock.getZone()));
    }
}
