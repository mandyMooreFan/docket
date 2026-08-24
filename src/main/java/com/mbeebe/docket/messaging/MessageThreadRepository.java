package com.mbeebe.docket.messaging;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface MessageThreadRepository extends Repository<MessageThread, Long> {

    MessageThread save(MessageThread thread);

    Optional<MessageThread> findById(long id);

    /** The pair, normalised by the caller — the unique index answers in one hop. */
    Optional<MessageThread> findByMemberAAndMemberB(long memberA, long memberB);

    List<MessageThread> findByMemberAOrMemberB(long memberA, long memberB);
}
