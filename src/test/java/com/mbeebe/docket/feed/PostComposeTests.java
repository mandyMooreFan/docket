package com.mbeebe.docket.feed;

import com.mbeebe.docket.graph.GraphTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §5.2.1: written Posts — long-form plain text, links, still images —
 * behind the §3.2 POST capability. Text is escaped wholesale (no HTML in
 * posts); a link renders as a safe anchor plus a domain-only preview line
 * (never a fetched card); images ride the one §10.4 image pipeline.
 */
class PostComposeTests extends GraphTestBase {

    private static final Pattern POST_URL = Pattern.compile("/posts/(\\d+)");

    @Autowired
    JdbcTemplate jdbc;

    /** Posts, expecting success, and returns the new Post's id from the redirect. */
    long compose(Cookie session, String body) throws Exception {
        clock.advance(Duration.ofMinutes(1));
        String redirect = mvc.perform(post("/posts").cookie(session).param("body", body))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        Matcher matcher = POST_URL.matcher(redirect);
        if (!matcher.find()) {
            throw new AssertionError("Compose did not land on a post page: " + redirect);
        }
        return Long.parseLong(matcher.group(1));
    }

    long postCountBy(long memberId) {
        return jdbc.queryForObject(
                "select count(*) from post where author_id = " + memberId, Long.class);
    }

    @Test
    void aCompleteMemberPostsAndThePostLivesOnTheirProfile() throws Exception {
        Cookie author = completeMember("feed-compose-a@example.org", "Compose Author");
        long postId = compose(author, "First words on the feed, set down at length.");

        mvc.perform(get("/posts/" + postId).cookie(author))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Compose Author")))
                .andExpect(content().string(
                        containsString("First words on the feed, set down at length.")));

        // §5.4: the Post lives on the author's Profile as a dated list.
        mvc.perform(get("/p/" + memberId(author)).cookie(author))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/posts/" + postId)))
                .andExpect(content().string(containsString("First words on the feed")));
    }

    @Test
    void postingIsACapabilityEarnedByCompleteness() throws Exception {
        Cookie fresh = signUpAndIn("feed-compose-gated@example.org");
        long freshId = memberId(fresh);
        mvc.perform(post("/posts").cookie(fresh).param("body", "Too soon."))
                .andExpect(status().isForbidden());
        assertThat(postCountBy(freshId)).isZero();

        mvc.perform(post("/posts").param("body", "Nobody."))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void theCeilingIsGenerousAndTheErrorHonest() throws Exception {
        Cookie author = completeMember("feed-compose-cap@example.org", "Ceiling Author");
        long authorId = memberId(author);
        mvc.perform(post("/posts").cookie(author).param("body", "x".repeat(40_001)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("40,000")));
        mvc.perform(post("/posts").cookie(author).param("body", "   "))
                .andExpect(status().isUnprocessableEntity());
        assertThat(postCountBy(authorId)).isZero();

        compose(author, "y".repeat(40_000));
        assertThat(postCountBy(authorId)).isEqualTo(1);
    }

    @Test
    void bodiesAreEscapedAndLinksBecomeSafeAnchorsWithADomainPreview() throws Exception {
        Cookie author = completeMember("feed-compose-esc@example.org", "Escape Author");
        long postId = compose(author, "Watch <script>alert('x')</script> carefully.\n\n"
                + "I wrote this up at https://blog.example.org/entry?a=1&b=2 today.");

        String page = mvc.perform(get("/posts/" + postId).cookie(author))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert"))))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andReturn().getResponse().getContentAsString();

        // The URL is an anchor, escaped in the attribute, and nofollow.
        assertThat(page).contains(
                "href=\"https://blog.example.org/entry?a=1&amp;b=2\"");
        assertThat(page).contains("rel=\"nofollow noopener\"");
        // §5.2.1: the preview is a domain, never a fetched card.
        assertThat(page).contains("blog.example.org");
    }

    @Test
    void imagesRideTheSharedImagePipeline() throws Exception {
        Cookie author = completeMember("feed-compose-img@example.org", "Image Author");
        byte[] png = "post-image-bytes".getBytes(StandardCharsets.UTF_8);
        clock.advance(Duration.ofMinutes(1));

        String redirect = mvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("images", "shot.png", "image/png", png))
                        .param("body", "A picture, and some words to go with it.")
                        .cookie(author))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        String page = mvc.perform(get(redirect).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher image = Pattern.compile("/images/(\\d+)").matcher(page);
        assertThat(image.find()).as("the post page shows the image").isTrue();
        mvc.perform(get(image.group(0)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(png));
    }

    @Test
    void aRefusedImageRefusesTheWholePost() throws Exception {
        Cookie author = completeMember("feed-compose-badimg@example.org", "Refused Author");
        long authorId = memberId(author);

        mvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("images", "big.png", "image/png",
                                new byte[512 * 1024 + 1]))
                        .param("body", "Words that must not survive their image.")
                        .cookie(author))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("images", "clip.gif", "image/gif",
                                new byte[16]))
                        .param("body", "A gif is not a still image we accept.")
                        .cookie(author))
                .andExpect(status().isUnprocessableEntity());

        assertThat(postCountBy(authorId)).isZero();
    }

    @Test
    void anUnknownPostIsNotFound() throws Exception {
        Cookie viewer = completeMember("feed-compose-404@example.org", "Nobody Home");
        mvc.perform(get("/posts/999999999").cookie(viewer))
                .andExpect(status().isNotFound());
    }
}
