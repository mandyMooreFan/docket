package com.mbeebe.docket.profile;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface PositionRepository extends Repository<Position, Long> {

    Position save(Position position);

    Optional<Position> findByIdAndMemberId(Long id, Long memberId);

    List<Position> findByMemberIdOrderByStartMonthDesc(Long memberId);

    long countByMemberId(Long memberId);

    void delete(Position position);

    @Query("select count(p) > 0 from Position p where p.memberId = :memberId "
            + "and p.company.id = :companyId and p.endMonth is null")
    boolean existsCurrentAt(@Param("memberId") long memberId, @Param("companyId") long companyId);

    @Query("select distinct p.memberId from Position p where p.company.id = :companyId "
            + "and p.endMonth is null order by p.memberId")
    List<Long> memberIdsCurrentlyAt(@Param("companyId") long companyId);

    @Query("select p from Position p where p.company.id = :companyId")
    List<Position> findByCompanyId(@Param("companyId") long companyId);
}
