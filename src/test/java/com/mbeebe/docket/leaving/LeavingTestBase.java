package com.mbeebe.docket.leaving;

import com.mbeebe.docket.messaging.MessagingTestBase;
import jakarta.servlet.http.Cookie;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plumbing for §11's two suites, on top of messaging's (which brings the graph,
 * the jobs board and the Thread machinery with it — an archive worth testing needs
 * all of them).
 *
 * <p>The Postgres container and GreenMail are shared across every suite in the
 * run, so every email here is prefixed and every assertion is member- or
 * entry-scoped, never a global count.
 */
public abstract class LeavingTestBase extends MessagingTestBase {

    /** The archive as a map of entry name to bytes — the shape every assertion wants. */
    protected Map<String, byte[]> archiveOf(Cookie session) throws Exception {
        byte[] zip = mvc.perform(get("/settings/data/export.zip").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }

    protected String text(Map<String, byte[]> archive, String entry) {
        byte[] bytes = archive.get(entry);
        if (bytes == null) {
            throw new AssertionError("No " + entry + " in the archive: " + archive.keySet());
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Everything readable in the archive at once, for "is this fact anywhere" asks. */
    protected String allDocuments(Map<String, byte[]> archive) {
        return text(archive, "README.txt")
                + text(archive, "your-docket.html")
                + text(archive, "your-messages.html")
                + text(archive, "about-this-copy.html")
                + text(archive, "docket-export.json");
    }

    /** §11.2's door, ticked. The tick is the only thing the server insists on. */
    protected void leave(Cookie session) throws Exception {
        mvc.perform(post("/settings/data/leave").param("confirm", "yes").cookie(session))
                .andExpect(redirectedUrl("/left"));
    }

    protected long setPhoto(Cookie session, byte[] bytes) throws Exception {
        mvc.perform(multipart("/profile/photo")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "photo", "me.png", "image/png", bytes))
                        .cookie(session))
                .andExpect(redirectedUrl("/profile/edit"));
        return photoIdOf(session);
    }

    /** Row-scoped, never a global count: this one member's photo pointer. */
    private long photoIdOf(Cookie session) throws Exception {
        String page = mvc.perform(get("/profile/edit").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern.compile("/images/(\\d+)").matcher(page);
        if (!matcher.find()) {
            throw new AssertionError("No photo on the edit page after upload");
        }
        return Long.parseLong(matcher.group(1));
    }

    protected void recommend(Cookie author, Cookie subject, String words) throws Exception {
        mvc.perform(post("/p/" + memberId(subject) + "/recommend").cookie(author)
                        .param("text", words))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/recommendations/" + memberId(author) + "/approve").cookie(subject))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * The feed's two helpers, restated rather than inherited: {@code FeedTestBase}
     * and {@code MessagingTestBase} are separate branches off {@code GraphTestBase},
     * and §11 is the first thing in the product that needs both at once. Same
     * clock-advance for the same reason — real clocks never hand two Posts the same
     * instant, but the test clock would.
     */
    protected long compose(Cookie session, String body) throws Exception {
        clock.advance(java.time.Duration.ofMinutes(1));
        String redirect = mvc.perform(post("/posts").cookie(session).param("body", body))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        var matcher = java.util.regex.Pattern.compile("/posts/(\\d+)").matcher(redirect);
        if (!matcher.find()) {
            throw new AssertionError("Compose did not land on a post page: " + redirect);
        }
        return Long.parseLong(matcher.group(1));
    }

    protected String feedSeenBy(Cookie session) throws Exception {
        return mvc.perform(get("/").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * A rendered page with its whitespace flattened. Copy assertions want the
     * sentence a member reads, not the line breaks a template author chose, and a
     * test that pins those is a test that fails on a reflow rather than on a
     * regression.
     */
    protected String flat(String page) {
        return page.replaceAll("\\s+", " ");
    }

    protected String profilePage(Cookie viewer, long memberId) throws Exception {
        return mvc.perform(get("/p/" + memberId).cookie(viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
