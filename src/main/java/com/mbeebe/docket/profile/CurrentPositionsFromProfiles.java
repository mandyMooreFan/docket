package com.mbeebe.docket.profile;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.company.Company;
import com.mbeebe.docket.company.CurrentPositions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The profile module's answer to {@link CurrentPositions}: currency is a null end
 * month read fresh every time (ADR-0002), never a flag.
 */
@Component
class CurrentPositionsFromProfiles implements CurrentPositions {

    private final PositionRepository positions;
    private final Companies companies;

    CurrentPositionsFromProfiles(PositionRepository positions, Companies companies) {
        this.positions = positions;
        this.companies = companies;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean heldBy(long memberId, long companyId) {
        return positions.existsCurrentAt(memberId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> membersAt(long companyId) {
        return positions.memberIdsCurrentlyAt(companyId);
    }

    @Override
    @Transactional
    public List<Long> repointAll(long fromCompanyId, long toCompanyId) {
        Company to = companies.find(toCompanyId).orElseThrow();
        return positions.findByCompanyId(fromCompanyId).stream()
                .map(position -> {
                    position.moveTo(to);
                    return position.id();
                })
                .toList();
    }
}
