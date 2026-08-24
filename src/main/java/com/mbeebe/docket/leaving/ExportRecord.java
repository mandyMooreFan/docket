package com.mbeebe.docket.leaving;

import java.util.List;

/**
 * One stored thing in the archive — a Position, a Post, a Thread — as an ordered
 * list of labelled facts, optional nested things, and optional images.
 *
 * <p>Deliberately generic, and the reason is ADR-0002 rather than laziness: the
 * archive is a copy of what is <em>stored</em>, and stored things are all the same
 * shape at this level — rows of facts. Conclusions are not in here at all, so
 * there is nothing for a richer type to carry. Nesting exists for exactly one
 * case that the spec names, a Thread and the Messages inside it (§11.1's "Threads
 * export whole, both halves").
 */
public record ExportRecord(String label, List<ExportField> fields,
                           List<ExportRecord> entries, List<ExportMedia> media) {

    public static ExportRecord of(String label, List<ExportField> fields) {
        return new ExportRecord(label, fields, List.of(), List.of());
    }

    public static ExportRecord of(String label, List<ExportField> fields,
                                  List<ExportMedia> media) {
        return new ExportRecord(label, fields, List.of(), media);
    }

    public static ExportRecord nesting(String label, List<ExportField> fields,
                                       List<ExportRecord> entries) {
        return new ExportRecord(label, fields, entries, List.of());
    }

    ExportRecord withMedia(List<ExportMedia> resolved, List<ExportRecord> resolvedEntries) {
        return new ExportRecord(label, fields, resolvedEntries, resolved);
    }
}
