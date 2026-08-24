package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.ImageAudience;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The company module's answer for /images/{id} (§8.4): a Company's logo is public.
 *
 * <p>§6.1 and §8.4 put the whole Company page on the open web — name, logo,
 * description, postings — and no Dial governs any of it, because a Company is never
 * an actor with a Dial to turn. That permanence is what earns the logo a shared,
 * immutable cache where nothing else gets one.
 *
 * <p>Only the logo a Company currently wears is claimed. A logo since replaced stops
 * being claimed and stops being served — the edit history records the change in words
 * and never renders the old bytes.
 */
@Component
class CompanyLogoAudience implements ImageAudience {

    private final CompanyRepository companies;

    CompanyLogoAudience(CompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Verdict> verdictFor(long imageId, Optional<Member> viewer) {
        return companies.existsByLogoImageId(imageId)
                ? Optional.of(Verdict.OPEN_WEB)
                : Optional.empty();
    }
}
