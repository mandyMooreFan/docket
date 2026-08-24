package com.mbeebe.docket.moderation;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * A difference hash of an image, for §10.4's second automation: the local blocklist of
 * images taken down under s.20A.
 *
 * <p>§10.4 is explicit that "substantially the same" implies <em>perceptual</em>
 * hashing, "a real step up from exact hashing, and not free", and this is the cheap end
 * of that step. The image is reduced to a 9×8 grey thumbnail and each pixel compared
 * with its right-hand neighbour, giving 64 bits that describe the gradients of the
 * picture rather than its bytes. Re-encoding, rescaling, mild cropping and a change of
 * format leave it close; a different photograph does not.
 *
 * <p>Written here rather than pulled in, deliberately. CONTRIBUTING's invariants keep
 * the dependency list short, and forty lines of arithmetic is a smaller thing to own
 * than a library in the upload path of every image on the service. The one dependency
 * §10.4 does accept in that path is the third-party CSAM matcher, which is named and
 * bounded and is somebody else's list.
 *
 * <p>Matching is a distance, not an equality — which is why the blocklist is scanned
 * rather than looked up, and why the unique index on the hash column is only dedupe.
 */
final class PerceptualHash {

    /** The thumbnail is 9 wide so that 8 comparisons per row give exactly 64 bits. */
    private static final int WIDTH = 9;
    private static final int HEIGHT = 8;

    /**
     * How many of the 64 bits may differ and still count as "substantially the same".
     *
     * <p>Ten is the conventional working threshold for a 64-bit difference hash, and
     * the direction of the error matters here: this list exists so that the person
     * depicted reports once rather than every time, so a near miss that lets a
     * re-upload through is a worse failure than a near hit that sends a legitimate
     * image to a human. It is a refusal to store, not a finding against anyone.
     */
    static final int SUBSTANTIALLY_THE_SAME = 10;

    private PerceptualHash() {
    }

    /**
     * The hash of an image, or empty when the bytes are not a picture we can read —
     * in which case the caller must not treat "no hash" as "no match".
     */
    static Optional<Long> of(byte[] image) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException | RuntimeException unreadable) {
            return Optional.empty();
        }
        if (source == null) {
            return Optional.empty();
        }
        BufferedImage grey = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        var graphics = grey.createGraphics();
        try {
            graphics.drawImage(source.getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH),
                    0, 0, null);
        } finally {
            graphics.dispose();
        }

        long hash = 0L;
        int bit = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH - 1; x++) {
                int left = grey.getRaster().getSample(x, y, 0);
                int right = grey.getRaster().getSample(x + 1, y, 0);
                if (left > right) {
                    hash |= 1L << bit;
                }
                bit++;
            }
        }
        return Optional.of(hash);
    }

    /** How many bits differ — the whole of the matching rule. */
    static int distance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    static boolean substantiallyTheSame(long a, long b) {
        return distance(a, b) <= SUBSTANTIALLY_THE_SAME;
    }
}
