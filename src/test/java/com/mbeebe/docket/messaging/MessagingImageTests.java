package com.mbeebe.docket.messaging;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §7.2: a Message carries text, links and still images, through the one
 * §10.4 upload pipeline. The read path matters just as much: correspondence is
 * private by construction (§10.2), and image ids are sequential, so /images/{id}
 * must hand a Message's bytes to the two people in the Thread and to nobody else
 * — asked of MessageImageAudience on every request (#51's port, §8.5).
 */
class MessagingImageTests extends MessagingTestBase {

    private static final Pattern IMAGE_URL = Pattern.compile("/images/(\\d+)");

    @Autowired
    JdbcTemplate jdbc;

    long messageCount(long one, long other) {
        return jdbc.queryForObject(
                "select count(*) from message m join thread t on m.thread_id = t.id "
                        + "where t.member_a = ? and t.member_b = ?",
                Long.class, Math.min(one, other), Math.max(one, other));
    }

    long imageCount() {
        return jdbc.queryForObject("select count(*) from image", Long.class);
    }

    private static MockMultipartFile png(String bytes) {
        return new MockMultipartFile("images", "shot.png", "image/png",
                bytes.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void anImageOnAMessageIsServedToTheTwoPeopleInTheThreadAndNobodyElse() throws Exception {
        Cookie nia = completeMember("msg-img-nia@example.org", "Nia Sender");
        Cookie oto = completeMember("msg-img-oto@example.org", "Oto Receiver");
        connect(nia, oto);
        long niaId = memberId(nia);
        long otoId = memberId(oto);
        byte[] bytes = "the-only-copy-of-these-bytes".getBytes(StandardCharsets.UTF_8);

        mvc.perform(multipart("/messages/" + otoId).param("body", "The drawing, as promised.")
                        .file(new MockMultipartFile("images", "shot.png", "image/png", bytes))
                        .cookie(nia))
                .andExpect(status().is3xxRedirection());

        String page = threadPage(oto, niaId);
        Matcher matcher = IMAGE_URL.matcher(page);
        assertThat(matcher.find()).as("the message shows its image").isTrue();
        String url = matcher.group(0);

        // Both participants may fetch it, and it is never cached publicly — a
        // Thread is not the open web, so nothing in one may ride a shared cache.
        for (Cookie participant : new Cookie[] {nia, oto}) {
            mvc.perform(get(url).cookie(participant))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"))
                    .andExpect(header().string("Cache-Control",
                            org.hamcrest.Matchers.containsString("private")))
                    .andExpect(content().bytes(bytes));
        }

        // §10.2: private is private by construction. A member outside the Thread
        // cannot reach the bytes, and neither can a visitor with no session at
        // all — 404 either way, with no placeholder to confirm the image exists.
        Cookie pim = completeMember("msg-img-pim@example.org", "Pim Outside");
        mvc.perform(get(url).cookie(pim)).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void anImageTheStoreWillNotTakeRefusesTheWholeMessage() throws Exception {
        Cookie quin = completeMember("msg-img-quin@example.org", "Quin Oversize");
        Cookie rhea = completeMember("msg-img-rhea@example.org", "Rhea Oversize");
        connect(quin, rhea);
        long quinId = memberId(quin);
        long rheaId = memberId(rhea);
        long imagesBefore = imageCount();
        long messagesBefore = messageCount(quinId, rheaId);

        mvc.perform(multipart("/messages/" + rheaId).param("body", "Too big")
                        .file(new MockMultipartFile("images", "big.png", "image/png",
                                new byte[512 * 1024 + 1]))
                        .cookie(quin))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(multipart("/messages/" + rheaId).param("body", "Wrong type")
                        .file(new MockMultipartFile("images", "cv.pdf", "application/pdf",
                                new byte[16]))
                        .cookie(quin))
                .andExpect(status().isUnprocessableEntity());

        // §7.2: no file attachments, no CV uploads — and nothing partial lands.
        assertThat(imageCount()).isEqualTo(imagesBefore);
        assertThat(messageCount(quinId, rheaId)).isEqualTo(messagesBefore);
    }

    @Test
    void aMessageCarriesAtMostFourImagesAndRefusesTheFifthWhole() throws Exception {
        Cookie sen = completeMember("msg-img-sen@example.org", "Sen Many");
        Cookie tam = completeMember("msg-img-tam@example.org", "Tam Many");
        connect(sen, tam);
        long senId = memberId(sen);
        long tamId = memberId(tam);
        long messagesBefore = messageCount(senId, tamId);

        mvc.perform(multipart("/messages/" + tamId).param("body", "Five is too many")
                        .file(png("one")).file(png("two")).file(png("three"))
                        .file(png("four")).file(png("five"))
                        .cookie(sen))
                .andExpect(status().isUnprocessableEntity());
        assertThat(messageCount(senId, tamId)).isEqualTo(messagesBefore);

        mvc.perform(multipart("/messages/" + tamId).param("body", "Four is fine")
                        .file(png("one")).file(png("two")).file(png("three")).file(png("four"))
                        .cookie(sen))
                .andExpect(status().is3xxRedirection());
        assertThat(messageCount(senId, tamId)).isEqualTo(messagesBefore + 1);
    }
}
