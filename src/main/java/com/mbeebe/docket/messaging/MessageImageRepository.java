package com.mbeebe.docket.messaging;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface MessageImageRepository extends Repository<MessageImage, Long> {

    MessageImage save(MessageImage image);

    List<MessageImage> findByMessageIdOrderByPosition(long messageId);

    /**
     * The read-path guard for private correspondence: this image is only
     * servable if it genuinely hangs off a Message in this Thread. Paired with
     * a participation check in the controller, that is the whole authorisation.
     */
    @Query("select count(i) from MessageImage i, Message m "
            + "where i.messageId = m.id and i.imageId = :imageId and m.threadId = :threadId")
    long countInThread(@Param("threadId") long threadId, @Param("imageId") long imageId);
}
