package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
class CompanyPageService {

    private final CurrentPositions currentPositions;
    private final ProfileService profiles;

    CompanyPageService(CurrentPositions currentPositions, ProfileService profiles) {
        this.currentPositions = currentPositions;
        this.profiles = profiles;
    }

    /**
     * §8.4–8.5 in one place: people appear only for a signed-in viewer, only from
     * current Positions, and only when the member's own effective visibility admits
     * this viewer — asking the profile module the same question the Profile page
     * asks, so the people list can never exceed the Dial.
     */
    @Transactional(readOnly = true)
    CompanyPage pageFor(Company company, Optional<Member> viewer) {
        List<CompanyPage.PersonCard> people = viewer.isEmpty() ? List.of()
                : currentPositions.membersAt(company.id()).stream()
                        .flatMap(memberId -> profiles.pageFor(memberId, viewer).stream())
                        .map(page -> new CompanyPage.PersonCard(page.memberId(),
                                page.named() ? page.name() : "A member",
                                page.headline(), page.initials()))
                        .toList();
        return new CompanyPage(company.id(), company.name(), initial(company.name()),
                company.description(), company.logoImageId(), viewer.isPresent(), people);
    }

    private static String initial(String name) {
        String stripped = name.strip();
        return stripped.isEmpty() ? "·" : stripped.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
