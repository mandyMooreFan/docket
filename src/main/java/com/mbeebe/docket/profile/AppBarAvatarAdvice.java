package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * The signed-in member's own photo, for §2's app-bar avatar.
 *
 * <p>It lives here rather than beside {@code identity}'s {@code currentMember} for a
 * dependency reason: a photo is Profile content (§4.1), and identity must not learn
 * about Profiles to draw a 32px circle. The layout already reads {@code
 * currentMember.initial} for the fallback; this adds the one address beside it.
 *
 * <p>Accepted cost: one primary-key lookup per rendered page. It is your own photo,
 * so no derivation is needed to decide you may see it — {@link ProfilePhotoAudience}
 * still answers for the bytes, and the owner always passes.
 */
@ControllerAdvice
class AppBarAvatarAdvice {

    private final ProfileService profiles;

    AppBarAvatarAdvice(ProfileService profiles) {
        this.profiles = profiles;
    }

    @ModelAttribute("viewerPhotoImageId")
    Long viewerPhotoImageId(HttpServletRequest request) {
        return CurrentMember.get(request)
                .flatMap(member -> profiles.photoOf(member.id()))
                .orElse(null);
    }
}
