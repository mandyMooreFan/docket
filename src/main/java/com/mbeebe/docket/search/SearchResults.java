package com.mbeebe.docket.search;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * What the results template gets — fully loaded, never an entity (§14.2).
 *
 * <p>Four groups, in a fixed order, and nothing in this record says how well
 * anything matched or why it is here: §8.2's ordering is a property of the
 * lists, not something the page argues about. There is deliberately no word for
 * relevance-to-you anywhere in it.
 *
 * @param asked        a usable query was given; false means the page invites
 *                     one rather than listing anything, because there is no
 *                     browse-all surface (§8.5)
 * @param peopleGated  §8.4: the viewer is signed out, so the People group
 *                     explains itself instead of pretending nobody matched
 * @param hiddenByGraphFilter people matched, and the ticked graph filter is
 *                     what emptied the group — §13.4 again: an empty part says
 *                     which empty it is, and "nobody by that name" would be a
 *                     lie told by a filter the seeker can untick
 */
public record SearchResults(String query, boolean connectedOnly, boolean asked,
                            boolean signedIn, boolean peopleGated,
                            boolean hiddenByGraphFilter,
                            List<PersonRow> people, List<CompanySearch.Hit> companies,
                            List<PostSearch.Hit> posts, List<PostingSearch.Hit> jobs) {

    /**
     * A people result. Mutuals are DISPLAYED and never reorder (§8.2) — they
     * ride along the row the impersonal order already put here, and are plain
     * names because a shared Connection is a fact about the graph, not a link
     * to click through.
     */
    public record PersonRow(PersonCard person, List<String> mutuals) {

        /** §8.2's display line, or blank when there is nothing shared to say. */
        public String mutualsLine() {
            return mutuals.isEmpty() ? "" : "You both know " + String.join(", ", mutuals);
        }
    }

    /** Nothing to ask: no query, or one below §8.5's floor. */
    static SearchResults unasked(String query, boolean connectedOnly, boolean signedIn) {
        return new SearchResults(query, connectedOnly, false, signedIn, !signedIn, false,
                List.of(), List.of(), List.of(), List.of());
    }

    /** §13.4's no-results state for the one group that has spec copy of its own. */
    public boolean noPeople() {
        return asked && !peopleGated && !hiddenByGraphFilter && people.isEmpty();
    }
}
