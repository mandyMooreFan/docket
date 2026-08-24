package com.mbeebe.docket.moderation;

/**
 * The member conduct policy (§10.6), as the only categories a Report can carry.
 *
 * <p>"Everything not enumerated is not an offence" is the sentence this enum exists to
 * make true. A reporter picks from this list because there is no other list; a
 * moderator upholding a Report is upholding one of these six and no others; the
 * transparency log (§10.3) counts by these and no others.
 *
 * <p>Items 5 and 6 are prohibited <em>for all users</em>, not merely for minors, and
 * that scope is load-bearing rather than stylistic: it is what disapplies the OSA's
 * mandatory age-assurance trigger under s.12(5) (§9.5). Narrowing either of them to
 * under-18s would quietly reopen a decision made three sections away.
 */
public enum ReportCategory {

    /** Illegal content (§10.6.1). */
    ILLEGAL_CONTENT("Illegal content"),

    /** Impersonating a specific real person — the only name offence (§10.6.2, §3.3). */
    IMPERSONATION("Impersonating a real person"),

    /** Harassment of a specific Member (§10.6.3). */
    HARASSMENT("Harassing someone"),

    /** Bulk or commercial spam (§10.6.4). */
    SPAM("Bulk or commercial spam"),

    /** Pornographic content — prohibited for all users (§10.6.5). */
    PORNOGRAPHY("Pornographic content"),

    /**
     * Content encouraging, promoting or providing instructions for suicide, self-harm
     * or eating disorders — prohibited for all users (§10.6.6).
     */
    SELF_HARM("Suicide, self-harm or eating disorders");

    private final String label;

    ReportCategory(String label) {
        this.label = label;
    }

    /** The category in the product's own words, for the form and the log. */
    public String label() {
        return label;
    }
}
