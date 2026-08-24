package com.mbeebe.docket.graph;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface ConnectionRepository extends Repository<Connection, Long> {

    Connection save(Connection connection);

    /** Pair lookups always pass the lower id first; the table stores them that way. */
    Optional<Connection> findByMemberAAndMemberB(Long memberA, Long memberB);

    boolean existsByMemberAAndMemberB(Long memberA, Long memberB);

    long countByMemberAOrMemberB(Long memberA, Long memberB);

    List<Connection> findByMemberAOrMemberBOrderByConnectedAtDesc(Long memberA, Long memberB);

    void delete(Connection connection);
}
