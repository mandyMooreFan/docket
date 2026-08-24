package com.mbeebe.docket.profile;

import java.util.List;

/**
 * What the graph (§4.2–4.3) contributes to a rendered Profile page: the connection
 * count, Mutuals, the full list where the viewer is allowed it, the one request
 * affordance the viewer gets, and the Recommendations. Assembled by the graph
 * module (#32); a view model, never entities. Deliberately nothing else: no
 * degrees, no badges, no counts beyond the one §4.2 names.
 */
public record ProfileGraph(int connectionCount, List<PersonCard> mutuals,
                           List<PersonCard> connections, Affordance affordance,
                           String incomingNote, boolean mayBlock, boolean mayRecommend,
                           List<RecommendationCard> recommendations,
                           List<RecommendationCard> awaitingApproval) {

    /**
     * The one control the viewer gets (§4.2). REQUEST_SENT deliberately covers a
     * declined request too: decline is silent, so the sender's view never moves.
     */
    public enum Affordance { NONE, CONNECT, REQUEST_SENT, RESPOND, CONNECTED }

    /**
     * One recommendation as the page shows it: the author, and their serif words.
     *
     * <p>{@code authorFormer} carries §11.2 to this surface — <em>Recommendations
     * you wrote stay published</em>, and they stay published attributed to a former
     * member with no link behind the name. The words are the subject's personal
     * data as much as the author's ({@code docs/data-rights.md} §3), which is why
     * the leaver takes their name off it rather than taking it down.
     */
    public record RecommendationCard(long authorId, String authorName, String text,
                                     boolean authorFormer) {
    }

    public boolean canConnect() {
        return affordance == Affordance.CONNECT;
    }

    public boolean requestSent() {
        return affordance == Affordance.REQUEST_SENT;
    }

    public boolean canRespond() {
        return affordance == Affordance.RESPOND;
    }

    public boolean isConnected() {
        return affordance == Affordance.CONNECTED;
    }

    /** §4.2: the full connection list is for the owner and their Connections only. */
    public boolean listVisible() {
        return connections != null;
    }

    /** The §4.2 count, with §13.4's honesty at zero. */
    public String connectionsLabel() {
        if (connectionCount == 0) {
            return "No connections yet.";
        }
        return connectionCount == 1 ? "1 connection" : connectionCount + " connections";
    }
}
