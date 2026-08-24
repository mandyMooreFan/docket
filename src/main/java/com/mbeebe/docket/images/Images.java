package com.mbeebe.docket.images;

import com.mbeebe.docket.identity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

/**
 * The one image store (§10.4): every module that accepts an upload — company logos
 * now (#34), feed and messaging images later (#33, #36) — stores through here, so
 * the hash checks are structurally in every path. Minimal on purpose: bytes in
 * Postgres, two content types, a hard size cap.
 */
@Service
public class Images {

    public static final int MAX_BYTES = 512 * 1024;
    private static final Set<String> TYPES = Set.of("image/png", "image/jpeg");

    public enum Outcome { STORED, REFUSED, TOO_LARGE, WRONG_TYPE }

    /** The stored id travels only with a STORED outcome. */
    public record Stored(Outcome outcome, Long imageId) {
    }

    /**
     * What a controller needs to serve one image to one viewer. {@code openWeb} is
     * not decoration: it decides the cache headers, and getting it wrong would put a
     * members-only image into a shared proxy cache.
     */
    public record ServedImage(String contentType, byte[] data, boolean openWeb) {
    }

    private final ImageRepository repository;
    private final ImageChecks checks;
    private final ImageAudiences audiences;
    private final Clock clock;

    Images(ImageRepository repository, ImageChecks checks, ImageAudiences audiences, Clock clock) {
        this.repository = repository;
        this.checks = checks;
        this.audiences = audiences;
        this.clock = clock;
    }

    /** Type, cap, then the §10.4 checks — a refused image never reaches a row. */
    @Transactional
    public Stored store(byte[] bytes, String contentType) {
        if (!TYPES.contains(contentType)) {
            return new Stored(Outcome.WRONG_TYPE, null);
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            return new Stored(Outcome.TOO_LARGE, null);
        }
        if (!checks.permits(bytes)) {
            return new Stored(Outcome.REFUSED, null);
        }
        Image image = repository.save(new Image(contentType, bytes, clock.instant()));
        return new Stored(Outcome.STORED, image.id());
    }

    /**
     * The bytes, if this viewer may have them (§8.5: the Dial is honoured on every
     * surface, and derived data never exceeds it). The audience is asked before the
     * row is read, so an image out of a viewer's reach is indistinguishable from one
     * that never existed — the Profile page's no-placeholder 404, applied to bytes.
     * There is deliberately no viewer-less {@code load}: no route can get at an
     * image without answering for who is asking.
     */
    @Transactional(readOnly = true)
    public Optional<ServedImage> serve(long id, Optional<Member> viewer) {
        ImageAudience.Verdict verdict = audiences.verdictFor(id, viewer);
        if (verdict == ImageAudience.Verdict.HIDDEN) {
            return Optional.empty();
        }
        return repository.findById(id)
                .map(image -> new ServedImage(image.contentType(), image.data(),
                        verdict == ImageAudience.Verdict.OPEN_WEB));
    }
}
