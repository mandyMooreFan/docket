package com.mbeebe.docket.graph;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface ConnectionRequestRepository extends Repository<ConnectionRequest, Long> {

    ConnectionRequest save(ConnectionRequest request);

    Optional<ConnectionRequest> findByRequesterIdAndRecipientIdAndState(
            Long requesterId, Long recipientId, ConnectionRequest.State state);

    boolean existsByRequesterIdAndRecipientIdAndState(
            Long requesterId, Long recipientId, ConnectionRequest.State state);

    List<ConnectionRequest> findByRecipientIdAndStateOrderBySentAt(
            Long recipientId, ConnectionRequest.State state);
}
