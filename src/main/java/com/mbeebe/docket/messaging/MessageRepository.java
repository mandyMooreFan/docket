package com.mbeebe.docket.messaging;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface MessageRepository extends Repository<Message, Long> {

    Message save(Message message);

    List<Message> findByThreadIdOrderByIdAsc(long threadId);

    Optional<Message> findFirstByThreadIdOrderByIdDesc(long threadId);

    /**
     * The Unread count (§7.4, CONTEXT.md) — the only count the product shows
     * anywhere, derived at every ask and never stored: Messages in this
     * Member's Threads, written by the other person, past this Member's own
     * read mark. The mark is a message id, so a shared instant can never hide
     * a Message behind another.
     */
    @Query("select count(m) from Message m, MessageThread t "
            + "where m.threadId = t.id and m.authorId <> :memberId "
            + "and (t.memberA = :memberId or t.memberB = :memberId) "
            + "and m.id > coalesce((select r.lastReadMessageId from ThreadRead r "
            + "                     where r.threadId = t.id and r.memberId = :memberId), 0)")
    long unreadFor(@Param("memberId") long memberId);
}
