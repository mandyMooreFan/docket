package com.mbeebe.docket.moderation;

import com.mbeebe.docket.images.ImageChecks;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * §10.4's second automation, on the upload path: the local blocklist of images taken
 * down under s.20A, checked before storage.
 *
 * <p>Behind the {@link ImageChecks} port that #34 already put in the upload path, not
 * beside it, so that every existing call site — Company logos, Post images, Message
 * images — is covered without any of them learning that this exists. The port's
 * contract is unchanged: false means the bytes never touch the database.
 *
 * <p>The first automation, CSAM hash-matching against a third-party list, composes at
 * this same point when deployment wires the provider (§10.4.1); the map keeps that
 * account and its credentials out of the build. What is here is the local half, which
 * needs nobody's account.
 *
 * <p>This is now the product's only {@link ImageChecks}. The permit-all placeholder #34
 * left behind said in its own Javadoc that it stood there until the real checks landed,
 * and it is gone rather than left beside this one: two beans of the port with a
 * conditional deciding between them would have made "which check actually ran" an
 * ordering question, on the upload path, where the answer matters most. Tests that need
 * a different answer still override it with a {@code @Primary} fake, exactly as they
 * did before.
 */
@Component
class BlocklistImageChecks implements ImageChecks {

    private final BlockedImageHashRepository blocked;

    BlocklistImageChecks(BlockedImageHashRepository blocked) {
        this.blocked = blocked;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean permits(byte[] image) {
        return PerceptualHash.of(image)
                .map(this::notSubstantiallyBlocked)
                // Bytes we cannot read as a picture are not bytes we can recognise, and
                // refusing them here would be a judgement this check has no business
                // making. Images' own type and size rules already ran.
                .orElse(true);
    }

    private boolean notSubstantiallyBlocked(long hash) {
        return blocked.findAll().stream()
                .noneMatch(entry -> PerceptualHash.substantiallyTheSame(entry.hash(), hash));
    }
}
