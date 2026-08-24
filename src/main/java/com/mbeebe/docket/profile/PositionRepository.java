package com.mbeebe.docket.profile;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

interface PositionRepository extends Repository<Position, Long> {

    Position save(Position position);

    Optional<Position> findByIdAndMemberId(Long id, Long memberId);

    List<Position> findByMemberIdOrderByStartMonthDesc(Long memberId);

    long countByMemberId(Long memberId);

    void delete(Position position);
}
