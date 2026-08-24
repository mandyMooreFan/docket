package com.mbeebe.docket.images;

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

    /** What a controller needs to serve one image. */
    public record StoredImage(String contentType, byte[] data) {
    }

    private final ImageRepository repository;
    private final ImageChecks checks;
    private final Clock clock;

    Images(ImageRepository repository, ImageChecks checks, Clock clock) {
        this.repository = repository;
        this.checks = checks;
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

    @Transactional(readOnly = true)
    public Optional<StoredImage> load(long id) {
        return repository.findById(id)
                .map(image -> new StoredImage(image.contentType(), image.data()));
    }
}
