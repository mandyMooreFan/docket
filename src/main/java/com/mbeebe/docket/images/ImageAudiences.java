package com.mbeebe.docket.images;

import com.mbeebe.docket.identity.Member;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Every {@link ImageAudience} contributor in the application, asked in turn.
 *
 * <p>The default is the load-bearing line: no claim means {@link
 * ImageAudience.Verdict#HIDDEN}. A row that exists but that nothing points at — an
 * upload whose attachment rolled back, a logo since replaced — is not a hole in the
 * open web (§8.5). A module that starts storing images and forgets to contribute a
 * contributor gets 404s, which is a visible bug; the alternative default would have
 * got it a silent leak.
 */
@Component
class ImageAudiences {

    // Resolved per call, not at construction: a contributor lives in the module that
    // owns the images, and that module stores through Images — so eager injection
    // would close a cycle (Images → contributor → Images) at startup. Asking at read
    // time is also the honest shape, since the answer is derived per request anyway.
    private final ObjectProvider<ImageAudience> contributors;

    ImageAudiences(ObjectProvider<ImageAudience> contributors) {
        this.contributors = contributors;
    }

    ImageAudience.Verdict verdictFor(long imageId, Optional<Member> viewer) {
        return contributors.orderedStream()
                .flatMap(contributor -> contributor.verdictFor(imageId, viewer).stream())
                .findFirst()
                .orElse(ImageAudience.Verdict.HIDDEN);
    }
}
