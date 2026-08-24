package com.mbeebe.docket.leaving;

/**
 * One stored fact, twice: {@code key} is what the JSON calls it, {@code label} is
 * what a person reads on the page. Both, because §11.1 asks for both — "JSON for
 * portability plus readable pages … not a developer artefact" — and a single name
 * would have to be wrong for one of them.
 */
public record ExportField(String key, String label, String value) {

    public static ExportField of(String key, String label, Object value) {
        return new ExportField(key, label, value == null ? "" : String.valueOf(value));
    }
}
