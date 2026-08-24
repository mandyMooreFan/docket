package com.mbeebe.docket.moderation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.4.2: the local blocklist of hashes of images taken down under s.20A,
 * checked on the same upload path as the CSAM match, before storage.
 *
 * <p>§10.4 notes that "substantially the same" implies <em>perceptual</em> hashing — "a
 * real step up from exact hashing, and not free" — so the tests that matter here are
 * the ones about near misses, not identical bytes. A blocklist that only caught exact
 * re-uploads would make the person depicted report again every time the picture was
 * saved out of one editor and into another, which is the whole thing this list exists
 * to prevent.
 */
class BlocklistTests extends ModerationTestBase {

    @Autowired
    IntimateImageService intimateImages;

    /** A deterministic picture: a gradient with a block in it, so the hash has structure. */
    private static byte[] picture(int width, int height, int shift, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            for (int x = 0; x < width; x++) {
                g.setColor(new Color((x * 255 / width), 90, 160));
                g.fillRect(x, 0, 1, height);
            }
            g.setColor(new Color(20, 20, 20));
            g.fillRect(width / 4 + shift, height / 4, width / 3, height / 3);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static byte[] differentPicture() throws IOException {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 200, 200);
            g.setColor(Color.BLACK);
            for (int i = 0; i < 200; i += 20) {
                g.fillRect(i, 0, 10, 200);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void theSamePictureRescaledAndReEncodedStillMatches() throws Exception {
        long original = PerceptualHash.of(picture(400, 400, 0, "png")).orElseThrow();
        long rescaledJpeg = PerceptualHash.of(picture(220, 220, 0, "jpg")).orElseThrow();

        // The point of a perceptual hash: the bytes are wholly different, the picture
        // is not. Exact hashing would call these two unrelated files.
        assertThat(PerceptualHash.substantiallyTheSame(original, rescaledJpeg)).isTrue();
    }

    @Test
    void aDifferentPictureDoesNotMatch() throws Exception {
        long one = PerceptualHash.of(picture(400, 400, 0, "png")).orElseThrow();
        long other = PerceptualHash.of(differentPicture()).orElseThrow();

        // The error that matters in this direction: a false match is a refusal to store
        // somebody's legitimate photograph.
        assertThat(PerceptualHash.substantiallyTheSame(one, other)).isFalse();
    }

    @Test
    void bytesThatAreNotAPictureHaveNoHashAndAreNotTreatedAsAMatch() {
        Optional<Long> hash = PerceptualHash.of("this is not an image".getBytes());

        // "No hash" must never collapse into "no match found, therefore blocked", nor
        // into a match. Images' own type and size rules are what refuse these.
        assertThat(hash).isEmpty();
    }

    @Test
    void aBlocklistedPictureCannotBeUploadedAgainEvenReEncoded() throws Exception {
        Cookie author = completeMember("mod-block-author@example.org", "Blocking Author");
        intimateImages.blocklist(picture(400, 400, 0, "png"), null);

        MockMultipartFile reupload = new MockMultipartFile(
                "images", "again.jpg", "image/jpeg", picture(220, 220, 0, "jpg"));

        // Refused before storage (§10.4), and the whole write rolls back with it.
        mvc.perform(multipart("/posts").file(reupload)
                        .param("body", "Posting it again in a different format.")
                        .cookie(author))
                .andExpect(status().isUnprocessableEntity());

        assertThat(feedSeenBy(author))
                .doesNotContain("Posting it again in a different format.");
    }

    @Test
    void anUnrelatedPictureIsStillAccepted() throws Exception {
        Cookie author = completeMember("mod-block-ok@example.org", "Unblocked Author");
        intimateImages.blocklist(picture(400, 400, 0, "png"), null);

        MockMultipartFile fine = new MockMultipartFile(
                "images", "mine.png", "image/png", differentPicture());

        mvc.perform(multipart("/posts").file(fine)
                        .param("body", "An entirely different picture.")
                        .cookie(author))
                .andExpect(status().is3xxRedirection());
    }
}
