package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.ImageAudience;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The profile module's answer for /images/{id} (§4.1, §8.5): a member's photo is
 * visible exactly when their Profile is.
 *
 * <p>Deliberately thin, in {@code feed.PostImageAudience}'s shape. It resolves the
 * image to the Profile that currently wears it and then asks {@link
 * ProfileService#pageFor}, the same derivation the Profile page itself runs — so the
 * Dial, both §4.1 floors (an incomplete Profile, §3.2; an under-18's, §9.2) and
 * §7.3's Blocks arrive without being restated. §8.5's rule is that the Dial is
 * honoured on <em>every</em> surface, and that only holds while every surface asks
 * the one question. A photo out of reach is a plain 404, no placeholder — the
 * Profile page's discipline, applied to bytes.
 *
 * <p><strong>Never {@link Verdict#OPEN_WEB}, not even for a public, complete adult
 * Profile</strong>, and that is a cache decision rather than a visibility one. The
 * logged-out reader is served; what they are not given is a shared, immutable cache
 * entry. #51 reserved {@code OPEN_WEB} for images that are public <em>permanently</em>
 * — a Company logo, which no Dial governs because a Company is never an actor with a
 * Dial to turn. A member's face is the opposite case: its audience is derived from a
 * setting they can turn down in the next second (ADR-0002), and a proxy holding the
 * bytes for a year would keep serving a face its owner had already withdrawn. So a
 * photo travels {@code no-store, private} on every hit, and revocation is instant.
 *
 * <p>Only the photo a Profile currently wears is claimed. Replace your photo and the
 * old row stops being anyone's, which — by {@code ImageAudiences}' fail-closed
 * default — stops it being served to anybody at all.
 */
@Component
class ProfilePhotoAudience implements ImageAudience {

    private final ProfileRepository profiles;
    private final ProfileService service;

    ProfilePhotoAudience(ProfileRepository profiles, ProfileService service) {
        this.profiles = profiles;
        this.service = service;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Verdict> verdictFor(long imageId, Optional<Member> viewer) {
        return profiles.findByPhotoImageId(imageId)
                .map(profile -> service.pageFor(profile.memberId(), viewer).isPresent()
                        ? Verdict.THIS_VIEWER
                        : Verdict.HIDDEN);
    }
}
