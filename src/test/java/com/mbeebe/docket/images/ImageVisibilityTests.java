package com.mbeebe.docket.images;

import com.mbeebe.docket.feed.FeedTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC.md §8.5 and §9.4 at the bytes: /images/{id} is a surface like any other, so
 * the Dial is honoured on it, and nothing authored by an under-18 is reachable
 * logged-out. {@code image.id} is a sequential identity column — trivially walkable —
 * which is exactly why the guard has to live at the route and not in the templates
 * that happen to link to it.
 */
class ImageVisibilityTests extends FeedTestBase {

    private static final Pattern IMAGE_URL = Pattern.compile("/images/(\\d+)");

    @Autowired
    JdbcTemplate jdbc;

    /** Writes a Post carrying one image and returns that image's id. */
    private long composeWithImage(Cookie session, String body, byte[] png) throws Exception {
        clock.advance(Duration.ofMinutes(1));
        String redirect = mvc.perform(multipart("/posts")
                        .file(new MockMultipartFile("images", "shot.png", "image/png", png))
                        .param("body", body)
                        .cookie(session))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        String page = mvc.perform(get(redirect).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = IMAGE_URL.matcher(page);
        if (!matcher.find()) {
            throw new AssertionError("The post page carries no image: " + redirect);
        }
        return Long.parseLong(matcher.group(1));
    }

    private void dial(Cookie session, String dial) throws Exception {
        mvc.perform(post("/profile/dial").cookie(session).param("dial", dial))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * §5.4 + §8.5: the image rides the Post, the Post rides the author's Dial. A
     * stranger and the open web get the Profile page's discipline — a plain 404, no
     * placeholder, nothing that confirms the bytes are there.
     */
    @Test
    void aConnectionsOnlyPostImageReachesTheAuthorAndTheirConnectionAndNobodyElse()
            throws Exception {
        Cookie author = completeMember("img-conn-author@example.org", "Connie Author");
        Cookie friend = completeMember("img-conn-friend@example.org", "Frank Friend");
        Cookie stranger = completeMember("img-conn-stranger@example.org", "Sandy Stranger");
        connect(friend, author);
        dial(author, "CONNECTIONS_ONLY");

        byte[] png = "connections-only-bytes".getBytes(StandardCharsets.UTF_8);
        long imageId = composeWithImage(author, "A picture for my connections.", png);

        mvc.perform(get("/images/" + imageId).cookie(author))
                .andExpect(status().isOk())
                .andExpect(content().bytes(png));
        mvc.perform(get("/images/" + imageId).cookie(friend))
                .andExpect(status().isOk())
                .andExpect(content().bytes(png));
        mvc.perform(get("/images/" + imageId).cookie(stranger))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + imageId))
                .andExpect(status().isNotFound());
    }

    /**
     * §8.5's other half: a private image never rides a shared cache. The Dial can
     * turn down in the next second (ADR-0002) and a proxy holding the bytes for a
     * year would outlive the change.
     */
    @Test
    void aPrivateImageIsNeverStoredByAnySharedCache() throws Exception {
        Cookie author = completeMember("img-cache-author@example.org", "Cassie Cache");
        dial(author, "MEMBERS_ONLY");
        long imageId = composeWithImage(author, "Members only, please.",
                "members-only-cache-bytes".getBytes(StandardCharsets.UTF_8));

        String cacheControl = mvc.perform(get("/images/" + imageId).cookie(author))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Cache-Control");
        assertThat(cacheControl).contains("no-store").contains("private");
        assertThat(cacheControl).doesNotContain("public");
    }

    /**
     * §9.4, absolutely: nothing authored by an under-18 is visible logged-out — not
     * the words, and not the picture beside them. The Dial says PUBLIC here and it
     * changes nothing.
     */
    @Test
    void aMinorAuthoredPostImageIsNeverOnTheOpenWeb() throws Exception {
        Cookie minor = completeMinor("img-minor-author@example.org", "Minnie Minor");
        dial(minor, "PUBLIC");
        byte[] png = "minor-authored-bytes".getBytes(StandardCharsets.UTF_8);
        long imageId = composeWithImage(minor, "A picture taken at seventeen.", png);

        mvc.perform(get("/images/" + imageId))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + imageId)
                        .cookie(completeMember("img-minor-viewer@example.org", "Adele Adult")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(png));
    }

    /** §7.3: a Block is total and symmetric — including at the bytes. */
    @Test
    void aBlockedPairCannotFetchEachOthersPostImages() throws Exception {
        Cookie blocker = completeMember("img-block-blocker@example.org", "Bea Blocker");
        Cookie blocked = completeMember("img-block-blocked@example.org", "Barry Blocked");
        byte[] blockersPng = "blockers-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] blockedsPng = "blockeds-bytes".getBytes(StandardCharsets.UTF_8);
        long blockersImage = composeWithImage(blocker, "Mine.", blockersPng);
        long blockedsImage = composeWithImage(blocked, "Also mine.", blockedsPng);

        // Both public a moment ago.
        mvc.perform(get("/images/" + blockersImage).cookie(blocked))
                .andExpect(status().isOk());
        mvc.perform(get("/images/" + blockedsImage).cookie(blocker))
                .andExpect(status().isOk());

        mvc.perform(post("/p/" + memberId(blocked) + "/block").cookie(blocker))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/images/" + blockersImage).cookie(blocked))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + blockedsImage).cookie(blocker))
                .andExpect(status().isNotFound());
        // Each still has their own.
        mvc.perform(get("/images/" + blockersImage).cookie(blocker))
                .andExpect(status().isOk());
    }

    /**
     * §8.5: "no enumeration surface exists beyond these". The adversary here does the
     * obvious thing — walks every id in the table — and must come away with none of
     * the bytes they were not already entitled to, logged-out or signed in as a
     * stranger. Asserting on the bytes rather than the status is deliberate: a leak
     * that came back 200 with somebody else's picture would pass a status check.
     */
    @Test
    void walkingTheWholeIdSpaceYieldsNothingTheViewerMayNotAlreadySee() throws Exception {
        Cookie author = completeMember("img-walk-author@example.org", "Wanda Walker");
        dial(author, "CONNECTIONS_ONLY");
        byte[] secret = "walked-and-never-found".getBytes(StandardCharsets.UTF_8);
        long secretImage = composeWithImage(author, "Not for the walkers.", secret);

        Cookie minor = completeMinor("img-walk-minor@example.org", "Milo Minor");
        dial(minor, "PUBLIC");
        byte[] minorsBytes = "walked-and-never-found-either".getBytes(StandardCharsets.UTF_8);
        long minorsImage = composeWithImage(minor, "Also not for the walkers.", minorsBytes);

        Cookie stranger = completeMember("img-walk-stranger@example.org", "Stan Stranger");
        List<Long> everyImage = jdbc.queryForList("select id from image order by id", Long.class);
        assertThat(everyImage).contains(secretImage, minorsImage);

        for (long id : everyImage) {
            byte[] toStranger = mvc.perform(get("/images/" + id).cookie(stranger))
                    .andReturn().getResponse().getContentAsByteArray();
            byte[] toTheOpenWeb = mvc.perform(get("/images/" + id))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(toStranger).as("id %d handed the stranger a private image", id)
                    .isNotEqualTo(secret);
            assertThat(toTheOpenWeb).as("id %d handed the open web a private image", id)
                    .isNotEqualTo(secret);
            // §9.4: the minor's picture is members-only however you reach for it.
            assertThat(toTheOpenWeb).as("id %d handed the open web a minor's image", id)
                    .isNotEqualTo(minorsBytes);
        }

        // The guard is a guard, not a wall: the owner and a member still get theirs.
        mvc.perform(get("/images/" + secretImage).cookie(author))
                .andExpect(status().isOk())
                .andExpect(content().bytes(secret));
        mvc.perform(get("/images/" + minorsImage).cookie(stranger))
                .andExpect(status().isOk())
                .andExpect(content().bytes(minorsBytes));
    }

    /**
     * A row nobody claims is served to nobody — not the open web, not a signed-in
     * member, not whoever's bytes they were. Fail-closed is the only safe default on
     * a sequential id space: an image whose attachment never landed must be a wall,
     * not a hole (§8.5).
     */
    @Test
    void anImageNothingPointsAtIsServedToNobody() throws Exception {
        Long orphan = jdbc.queryForObject("""
                insert into image (content_type, data, created_at)
                values ('image/png', ?, now()) returning id
                """, Long.class, (Object) "orphaned-bytes".getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/images/" + orphan))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + orphan)
                        .cookie(completeMember("img-orphan-viewer@example.org", "Orla Orphan")))
                .andExpect(status().isNotFound());
    }
}
