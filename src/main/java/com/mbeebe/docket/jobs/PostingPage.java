package com.mbeebe.docket.jobs;

import com.mbeebe.docket.profile.PersonCard;

/**
 * The posting page (§6.3) — salary at the top, the posting member named and
 * linked (accountability is a person), and the one apply affordance, which
 * simply reports the same checks the server enforces (§3.2).
 */
public record PostingPage(long id, String title, String salaryLine, String placeLine,
                          String companyName, long companyId, String description,
                          PersonCard poster, String postedOn, String closesOn,
                          boolean open, ApplyBox apply, boolean mayShare) {

    public record ApplyBox(Kind kind, String stateLabel, String appliedOn,
                           java.util.List<String> missing) {

        public enum Kind { SIGNED_OUT, INCOMPLETE, OPEN, APPLIED, CLOSED, YOURS }

        public boolean is(String name) {
            return kind.name().equals(name);
        }
    }
}
