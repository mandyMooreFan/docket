package com.mbeebe.docket.images;

import com.mbeebe.docket.DocketTestBase;
import com.icegreen.greenmail.util.GreenMailUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §10.4: the two hash checks run on every image upload, BEFORE storage —
 * the product's first upload path builds the seam. The fake here stands where the
 * CSAM hash-match and the s.20A blocklist will stand, and proves the load-bearing
 * property: a rejected image never reaches the database.
 */
@Import(ImageUploadTests.RejectingChecks.class)
class ImageUploadTests extends DocketTestBase {

    /** Rejects any image carrying the marker bytes — the test's stand-in blocklist. */
    @TestConfiguration
    static class RejectingChecks {
        @Bean
        @Primary
        ImageChecks rejectingImageChecks() {
            return image -> !new String(image, StandardCharsets.ISO_8859_1).contains("BLOCKED");
        }
    }

    private static final Pattern VERIFY_LINK = Pattern.compile("/verify/([A-Za-z0-9_-]+)");
    private static final Pattern IMAGE_URL = Pattern.compile("/images/(\\d+)");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.mbeebe.docket.company.Companies companies;

    /**
     * This class runs in its own Spring context (the fake above), whose clock starts
     * at real time while the shared database's rate-limit ledger already carries
     * rows stamped by the base context's far-advanced clock. Signing up from a
     * distinct IP keeps the two contexts' per-IP windows out of each other's way —
     * in both directions.
     */
    Cookie signUpAndInFromOwnIp(String email) throws Exception {
        mvc.perform(post("/join/link")
                        .param("email", email)
                        .param("ageKind", "ADULT")
                        .with(request -> {
                            request.setRemoteAddr("10.99.34.1");
                            return request;
                        }))
                .andExpect(status().isOk());
        return sessionCookieFor(latestMailedToken());
    }

    /** A member past the trust gate at a fresh company — the only one who may upload. */
    Cookie gatedEditorAt(String email, String name, String company, String domain)
            throws Exception {
        Cookie session = signUpAndInFromOwnIp(email);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", company)
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        long companyId = companies.named(company).id();
        mvc.perform(post("/companies/" + companyId + "/verify").cookie(session)
                        .param("address", "editor@" + domain))
                .andExpect(status().is3xxRedirection());
        var messages = greenMail.getReceivedMessages();
        Matcher matcher = VERIFY_LINK.matcher(
                GreenMailUtil.getBody(messages[messages.length - 1]));
        if (!matcher.find()) {
            throw new AssertionError("No verify link");
        }
        mvc.perform(post("/verify").param("token", matcher.group(1)))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    long imageCount() {
        return jdbc.queryForObject("select count(*) from image", Long.class);
    }

    @Test
    void aLogoUploadStoresServesAndEntersTheEditHistory() throws Exception {
        Cookie editor = gatedEditorAt("co-img-ok@example.org", "Logan Upp",
                "Pixelworks IMG", "pixelworks-img.example");
        long company = companies.named("Pixelworks IMG").id();
        byte[] png = "not-really-a-png-but-bytes-are-bytes".getBytes(StandardCharsets.UTF_8);

        mvc.perform(multipart("/companies/" + company + "/logo")
                        .file(new MockMultipartFile("logo", "logo.png", "image/png", png))
                        .cookie(editor))
                .andExpect(redirectedUrl("/companies/" + company));

        String page = mvc.perform(get("/companies/" + company))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = IMAGE_URL.matcher(page);
        assertThat(matcher.find()).as("the page shows the logo").isTrue();

        mvc.perform(get(matcher.group(0)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(png));

        // The change is history like any other (§6.1).
        assertThat(jdbc.queryForObject("""
                select count(*) from company_edit
                where company_id = %d and field = 'LOGO'
                """.formatted(company), Long.class)).isEqualTo(1);
    }

    @Test
    void aRejectedHashNeverGetsStored() throws Exception {
        Cookie editor = gatedEditorAt("co-img-rej@example.org", "Reggie Ject",
                "Refused IMG", "refused-img.example");
        long company = companies.named("Refused IMG").id();
        long before = imageCount();

        mvc.perform(multipart("/companies/" + company + "/logo")
                        .file(new MockMultipartFile("logo", "logo.png", "image/png",
                                "prefix-BLOCKED-suffix".getBytes(StandardCharsets.UTF_8)))
                        .cookie(editor))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("refused by the upload checks")));

        assertThat(imageCount()).isEqualTo(before);
    }

    @Test
    void sizeAndTypeAreCappedBeforeStorage() throws Exception {
        Cookie editor = gatedEditorAt("co-img-cap@example.org", "Cappy Big",
                "Oversize IMG", "oversize-img.example");
        long company = companies.named("Oversize IMG").id();
        long before = imageCount();

        mvc.perform(multipart("/companies/" + company + "/logo")
                        .file(new MockMultipartFile("logo", "big.png", "image/png",
                                new byte[512 * 1024 + 1]))
                        .cookie(editor))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(multipart("/companies/" + company + "/logo")
                        .file(new MockMultipartFile("logo", "logo.gif", "image/gif",
                                new byte[16]))
                        .cookie(editor))
                .andExpect(status().isUnprocessableEntity());

        assertThat(imageCount()).isEqualTo(before);
    }

    @Test
    void onlyAGatedEditorMayUpload() throws Exception {
        gatedEditorAt("co-img-gate@example.org", "Gaia Ted",
                "Gatedlogo IMG", "gatedlogo-img.example");
        long company = companies.named("Gatedlogo IMG").id();

        mvc.perform(multipart("/companies/" + company + "/logo")
                        .file(new MockMultipartFile("logo", "logo.png", "image/png",
                                new byte[16]))
                        .cookie(signUpAndInFromOwnIp("co-img-stranger@example.org")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownImageIsNotFound() throws Exception {
        mvc.perform(get("/images/999999999")).andExpect(status().isNotFound());
    }
}
