package com.mbeebe.docket.messaging;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface ThreadReadRepository extends Repository<ThreadRead, ThreadRead.Key> {

    ThreadRead save(ThreadRead mark);

    Optional<ThreadRead> findByThreadIdAndMemberId(long threadId, long memberId);
}
