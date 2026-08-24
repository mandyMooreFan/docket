package com.mbeebe.docket.graph;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/** What the /network template gets (§4.2, §13.4) — cards, never entities. */
public record NetworkPage(List<RequestCard> pending, List<PersonCard> connections) {

    /** One pending incoming request: who asks, and the note they wrote (serif). */
    public record RequestCard(PersonCard from, String note) {
    }
}
