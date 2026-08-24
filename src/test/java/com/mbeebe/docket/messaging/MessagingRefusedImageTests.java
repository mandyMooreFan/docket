package com.mbeebe.docket.messaging;

import com.mbeebe.docket.images.ImageChecks;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.4 through the messaging path: the hash checks run BEFORE storage
 * on every upload route in the product, so a refused image never reaches a row —
 * and, here, never reaches a Message either. The fake below stands where the
 * CSAM hash-match and the s.20A blocklist will stand, exactly as
 * ImageUploadTests uses it for the company-logo path.
 */
@Import(MessagingRefusedImageTests.RejectingChecks.class)
class MessagingRefusedImageTests extends MessagingTestBase {

    @TestConfiguration
    static class RejectingChecks {
        @Bean
        @Primary
        ImageChecks rejectingImageChecks() {
            return image -> !new String(image, StandardCharsets.ISO_8859_1).contains("BLOCKED");
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    /**
     * This class runs in its own Spring context (the fake above), whose clock
     * starts at real time while the shared database's rate-limit ledger already
     * carries rows stamped by the base context's far-advanced clock. Signing up
     * from a distinct IP keeps the two contexts' per-IP windows apart — the same
     * hard-won trick ImageUploadTests documents.
     */
    private Cookie completeMemberFromOwnIp(String email, String name) throws Exception {
        mvc.perform(post("/join/link").param("email", email).param("ageKind", "ADULT")
                        .with(request -> {
                            request.setRemoteAddr("10.99.36.1");
                            return request;
                        }))
                .andExpect(status().isOk());
        Cookie session = sessionCookieFor(latestMailedToken());
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", "")
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    @Test
    void anImageTheChecksRefuseNeverBecomesAMessage() throws Exception {
        Cookie uma = completeMemberFromOwnIp("msg-hash-uma@example.org", "Uma Checked");
        Cookie vic = completeMemberFromOwnIp("msg-hash-vic@example.org", "Vic Checked");
        connect(uma, vic);
        long umaId = memberId(uma);
        long vicId = memberId(vic);
        long imagesBefore = jdbc.queryForObject("select count(*) from image", Long.class);

        mvc.perform(multipart("/messages/" + vicId).param("body", "Look at this")
                        .file(new MockMultipartFile("images", "shot.png", "image/png",
                                "prefix-BLOCKED-suffix".getBytes(StandardCharsets.UTF_8)))
                        .cookie(uma))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("refused by the upload checks")));

        // Nothing partial: no image row, and no Message to hang one off.
        assertThat(jdbc.queryForObject("select count(*) from image", Long.class))
                .isEqualTo(imagesBefore);
        assertThat(jdbc.queryForObject(
                "select count(*) from message m join thread t on m.thread_id = t.id "
                        + "where t.member_a = ? and t.member_b = ?",
                Long.class, Math.min(umaId, vicId), Math.max(umaId, vicId))).isZero();
    }
}
