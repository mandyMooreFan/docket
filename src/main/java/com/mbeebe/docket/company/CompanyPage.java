package com.mbeebe.docket.company;

import java.util.List;

/**
 * The Company page as one viewer may see it (§6.1, §8.4) — fully loaded, never an
 * entity (§14.2). Logged-out carries no people at all: the list is account-gated,
 * so its absence is structural, not a template rule to remember.
 */
public record CompanyPage(long id, String name, String initial, String description,
                          Long logoImageId, boolean signedIn, List<PersonCard> people) {

    public boolean hasLogo() {
        return logoImageId != null;
    }

    /** §13.4: the people list says what it is and never pads. */
    public String peopleLine() {
        if (people.isEmpty()) {
            return "No profiles list a current position here yet.";
        }
        if (people.size() == 1) {
            return "1 member works here.";
        }
        return people.size() + " members work here.";
    }

    /** One entry in the people list, already filtered by the viewer's rights (§8.5). */
    public record PersonCard(long memberId, String name, String headline, String initials) {
    }
}
