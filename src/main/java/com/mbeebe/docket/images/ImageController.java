package com.mbeebe.docket.images;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * Serves stored images behind the visibility of whatever they are on (§8.5).
 *
 * <p>The id is sequential and trivially walkable, so this route is an enumeration
 * surface unless every request carries a viewer and every image has an owner willing
 * to vouch for that viewer ({@link ImageAudience}). A viewer who may not have the
 * bytes gets a plain 404 — no placeholder, nothing that confirms the image exists.
 *
 * <p>Caching follows the audience, not the row. Rows are immutable, so a genuinely
 * public image (a Company logo, §8.4) is safe to keep forever in a shared cache.
 * Everything else is derived per viewer from a Dial that can turn down at any moment
 * (ADR-0002): {@code no-store, private}, so no proxy — and no shared browser profile —
 * can hand it to the next person to ask.
 */
@Controller
class ImageController {

    private static final CacheControl FOREVER =
            CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();
    private static final CacheControl NEVER = CacheControl.noStore().cachePrivate();

    private final Images images;

    ImageController(Images images) {
        this.images = images;
    }

    @GetMapping("/images/{id}")
    ResponseEntity<byte[]> serve(@PathVariable long id, HttpServletRequest request) {
        Images.ServedImage image = images.serve(id, CurrentMember.get(request))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(image.openWeb() ? FOREVER : NEVER)
                .body(image.data());
    }
}
