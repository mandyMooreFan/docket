package com.mbeebe.docket.messaging;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface MessageImageRepository extends Repository<MessageImage, Long> {

    MessageImage save(MessageImage image);

    List<MessageImage> findByMessageIdOrderByPosition(long messageId);

    /** What {@link MessageImageAudience} walks back from a bare image id. */
    Optional<MessageImage> findFirstByImageId(long imageId);
}
