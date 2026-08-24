package com.mbeebe.docket.graph;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.PersonCard;
import com.mbeebe.docket.profile.ProfileGraph;
import com.mbeebe.docket.profile.ProfileGraphLookup;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Assembles the §4.2–4.3 section of a Profile page. Every field is derived at
 * read time from the stored rows (ADR-0002); the affordance derivation is where
 * two rules quietly hold: a declined request keeps rendering as sent (§4.2), and
 * an adult viewing an under-18 is never offered the request affordance (§9.2).
 */
@Service
class ProfileGraphService implements ProfileGraphLookup {

    private final Connections graph;
    private final ConnectionRequestRepository requests;
    private final RecommendationRepository recommendations;
    private final ProfileService profiles;
    private final CapabilityService capabilities;
    private final Members members;

    ProfileGraphService(Connections graph, ConnectionRequestRepository requests,
                        RecommendationRepository recommendations, ProfileService profiles,
                        CapabilityService capabilities, Members members) {
        this.graph = graph;
        this.requests = requests;
        this.recommendations = recommendations;
        this.profiles = profiles;
        this.capabilities = capabilities;
        this.members = members;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileGraph onProfile(long ownerId, Optional<Member> viewer) {
        boolean owner = viewer.map(member -> member.id() == ownerId).orElse(false);
        boolean connectedViewer = !owner && viewer.isPresent()
                && graph.connected(ownerId, viewer.get().id());

        List<PersonCard> mutuals = viewer.isPresent() && !owner
                ? cards(graph.mutuals(ownerId, viewer.get().id()))
                : List.of();
        // §4.2: the full connection list is for the owner and their Connections only.
        List<PersonCard> connections = owner || connectedViewer
                ? cards(graph.connectedTo(ownerId))
                : null;

        ProfileGraph.Affordance affordance = ProfileGraph.Affordance.NONE;
        String incomingNote = "";
        if (viewer.isPresent() && !owner) {
            long viewerId = viewer.get().id();
            if (connectedViewer) {
                affordance = ProfileGraph.Affordance.CONNECTED;
            } else {
                Optional<ConnectionRequest> incoming =
                        requests.findByRequesterIdAndRecipientIdAndState(
                                ownerId, viewerId, ConnectionRequest.State.PENDING);
                if (incoming.isPresent()) {
                    affordance = ProfileGraph.Affordance.RESPOND;
                    incomingNote = incoming.get().note();
                } else if (requests.existsByRequesterIdAndRecipientIdAndState(
                                viewerId, ownerId, ConnectionRequest.State.PENDING)
                        || requests.existsByRequesterIdAndRecipientIdAndState(
                                viewerId, ownerId, ConnectionRequest.State.DECLINED)) {
                    // A declined request keeps looking sent, forever: decline is silent.
                    affordance = ProfileGraph.Affordance.REQUEST_SENT;
                } else if (maySend(viewer.get(), ownerId)) {
                    affordance = ProfileGraph.Affordance.CONNECT;
                }
            }
        }

        List<Recommendation> ofSubject = recommendations.findBySubjectIdOrderByWrittenAt(ownerId);
        List<ProfileGraph.RecommendationCard> displayed = ofSubject.stream()
                .filter(Recommendation::displayed).map(this::card).toList();
        List<ProfileGraph.RecommendationCard> awaiting = owner
                ? ofSubject.stream().filter(Recommendation::awaitingApproval)
                        .map(this::card).toList()
                : List.of();

        return new ProfileGraph(graph.countFor(ownerId), mutuals, connections, affordance,
                incomingNote, viewer.isPresent() && !owner, connectedViewer, displayed,
                awaiting);
    }

    /**
     * Whether to offer the request affordance at all — the same rules the server
     * enforces on the POST (§9.2, §3.2), withheld in the UI where we can know.
     */
    private boolean maySend(Member viewer, long ownerId) {
        boolean ownerIsMinor = members.find(ownerId).map(Member::isMinor).orElse(false);
        if (!viewer.isMinor() && ownerIsMinor) {
            return false;
        }
        return capabilities.may(viewer.id(), Capability.CONNECT) == CapabilityAnswer.YES;
    }

    private List<PersonCard> cards(List<Long> memberIds) {
        return memberIds.stream().map(profiles::cardFor).toList();
    }

    private ProfileGraph.RecommendationCard card(Recommendation recommendation) {
        PersonCard author = profiles.cardFor(recommendation.authorId());
        return new ProfileGraph.RecommendationCard(recommendation.authorId(),
                author.displayName(), recommendation.text(), author.former());
    }
}
