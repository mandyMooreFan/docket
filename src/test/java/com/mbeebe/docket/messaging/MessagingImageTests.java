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
 * §10.4 upload pipeline. The read path is the part that is messaging's own
 * problem: correspondence is private by construction (§10.2), so a Message's
 * image is served only to the two people in the Thread, and never from the
 * shared /images path where an id can simply be guessed.
 */
class MessagingImageTests extends MessagingTestBase {

    private static final Pattern MESSAGE_IMAGE =
            Pattern.compile("/messages/(\\d+)/images/(\\d+)");

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

        // The recipient's page points at the thread-scoped route, not /images/{id}.
        String page = threadPage(oto, niaId);
        Matcher matcher = MESSAGE_IMAGE.matcher(page);
        assertThat(matcher.find()).as("the message shows its image").isTrue();
        String url = matcher.group(0);
        long imageId = Long.parseLong(matcher.group(2));
        assertThat(page).doesNotContain("src=\"/images/");

        // Both participants may fetch it, and it is never cached publicly.
        for (Cookie participant : new Cookie[] {nia, oto}) {
            long other = participant == nia ? otoId : niaId;
            mvc.perform(get("/messages/" + other + "/images/" + imageId).cookie(participant))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"))
                    .andExpect(header().string("Cache-Control",
                            org.hamcrest.Matchers.containsString("private")))
                    .andExpect(content().bytes(bytes));
        }

        // §10.2: private is private by construction. A member outside the Thread
        // cannot reach the bytes, by either end of the pair — and neither can a
        // visitor with no session at all. 404, with no placeholder.
        Cookie pim = completeMember("msg-img-pim@example.org", "Pim Outside");
        mvc.perform(get(url).cookie(pim)).andExpect(status().isNotFound());
        mvc.perform(get("/messages/" + otoId + "/images/" + imageId).cookie(pim))
                .andExpect(status().isNotFound());
        mvc.perform(get("/messages/" + niaId + "/images/" + imageId).cookie(pim))
                .andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isNotFound());

        // An image that is real but belongs to no Message in this Thread is a 404
        // too: participation alone is not enough.
        mvc.perform(get("/messages/" + niaId + "/images/999999999").cookie(oto))
                .andExpect(status().isNotFound());
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
