package com.mbeebe.docket.company;

/** One history row as the template renders it (§6.1): who, what, from, to, when. */
public record CompanyEditEntry(long editorId, String editor, String field,
                               String from, String to, String when) {

    /** Logo values are image ids — the history says the logo changed, not the numbers. */
    public boolean showsValues() {
        return !"logo".equals(field);
    }
}
