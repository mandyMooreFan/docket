package com.mbeebe.docket.profile;

import com.mbeebe.docket.graph.GraphTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The member photo (SPEC.md §4.1, ticket #52): it is Profile content, so it rides
 * the Profile's Dial and both §4.1 floors at the bytes, and it is deliberately not
 * on the §3.2 bar, so it can never become a gate.
 *
 * <p>Every photo here is stored through the one image store (§10.4) and served by
 * /images/{id} — there is no second upload path and no second serving route, which
 * is why these tests fetch image URLs rather than anything photo-specific.
 */
class MemberPhotoTests extends GraphTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CapabilityService capabilities;

    /** Uploads a photo through /profile/edit's one form and returns its stored id. */
    private long setPhoto(Cookie session, byte[] bytes) throws Exception {
        mvc.perform(multipart("/profile/photo")
                        .file(new MockMultipartFile("photo", "me.png", "image/png", bytes))
                        .cookie(session))
                .andExpect(redirectedUrl("/profile/edit"));
        Long stored = storedPhotoId(session);
        assertThat(stored).as("the upload left a photo on the profile").isNotNull();
        return stored;
    }

    /** Row-scoped, never a global count: this one member's photo pointer. */
    private Long storedPhotoId(Cookie session) throws Exception {
        return jdbc.queryForObject("select photo_image_id from profile where member_id = ?",
                Long.class, memberId(session));
    }

    private String ownPage(Cookie session) throws Exception {
        return mvc.perform(get("/p/" + memberId(session)).cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void dial(Cookie session, String dial) throws Exception {
        mvc.perform(post("/profile/dial").cookie(session).param("dial", dial))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * The whole lifecycle in one pass, because removal is the half that usually rots:
     * set, replace, remove, and initials back on the page. §2's fallback is not a
     * placeholder waiting to be filled — it is where a Profile legitimately rests.
     */
    @Test
    void aPhotoIsSetReplacedAndRemovedAndTheInitialsComeBack() throws Exception {
        Cookie member = completeMember("photo-cycle@example.org", "Pia Cycle");

        assertThat(ownPage(member)).contains(">PC<").doesNotContain("/images/");

        byte[] first = "photo-cycle-first".getBytes(StandardCharsets.UTF_8);
        long firstId = setPhoto(member, first);
        assertThat(ownPage(member)).contains("/images/" + firstId);

        byte[] second = "photo-cycle-second".getBytes(StandardCharsets.UTF_8);
        long secondId = setPhoto(member, second);
        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(ownPage(member)).contains("/images/" + secondId)
                .doesNotContain("/images/" + firstId);
        mvc.perform(get("/images/" + secondId).cookie(member))
                .andExpect(status().isOk())
                .andExpect(content().bytes(second));
        // The replaced image is nobody's now, so it is served to nobody — the same
        // rule a replaced Company logo lives under, and the fail-closed default.
        mvc.perform(get("/images/" + firstId).cookie(member))
                .andExpect(status().isNotFound());

        mvc.perform(post("/profile/photo/delete").cookie(member))
                .andExpect(redirectedUrl("/profile/edit"));
        assertThat(storedPhotoId(member)).isNull();
        assertThat(ownPage(member)).contains(">PC<").doesNotContain("/images/");
        mvc.perform(get("/images/" + secondId).cookie(member))
                .andExpect(status().isNotFound());
    }

    /**
     * §4.1 + §8.5: the photo is on the page, so it has the page's audience. The
     * stranger and the open web get the Profile page's discipline at the bytes — a
     * plain 404, no placeholder, nothing that confirms a picture is there at all.
     */
    @Test
    void aConnectionsOnlyPhotoReachesTheOwnerAndTheirConnectionAndNobodyElse()
            throws Exception {
        Cookie owner = completeMember("photo-conn-owner@example.org", "Connie Owner");
        Cookie friend = completeMember("photo-conn-friend@example.org", "Frank Friend");
        Cookie stranger = completeMember("photo-conn-stranger@example.org", "Sandy Stranger");
        connect(friend, owner);
        dial(owner, "CONNECTIONS_ONLY");

        byte[] face = "connections-only-face".getBytes(StandardCharsets.UTF_8);
        long photo = setPhoto(owner, face);

        mvc.perform(get("/images/" + photo).cookie(owner))
                .andExpect(status().isOk())
                .andExpect(content().bytes(face));
        mvc.perform(get("/images/" + photo).cookie(friend))
                .andExpect(status().isOk())
                .andExpect(content().bytes(face));
        mvc.perform(get("/images/" + photo).cookie(stranger))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + photo))
                .andExpect(status().isNotFound());
    }

    /**
     * §3.2's floor, at the bytes: an incomplete Profile is members-only regardless of
     * the Dial, and so is the face on it. The Dial below says PUBLIC and changes
     * nothing — which is the point of a service-imposed floor.
     */
    @Test
    void anIncompleteProfilesPhotoIsNotOnTheOpenWeb() throws Exception {
        Cookie incomplete = signUpAndIn("photo-incomplete@example.org");
        dial(incomplete, "PUBLIC");
        byte[] face = "incomplete-face".getBytes(StandardCharsets.UTF_8);
        long photo = setPhoto(incomplete, face);

        mvc.perform(get("/images/" + photo))
                .andExpect(status().isNotFound());
        // Members-only, not hidden: a signed-in member still gets it.
        mvc.perform(get("/images/" + photo)
                        .cookie(completeMember("photo-incomplete-viewer@example.org", "Vic Viewer")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(face));
    }

    /**
     * §9.2's floor, same shape: an under-18's Profile is members-only regardless of
     * the Dial. A child's face is never on the open web.
     */
    @Test
    void anUnderEighteensPhotoIsNeverOnTheOpenWeb() throws Exception {
        Cookie minor = completeMinor("photo-minor@example.org", "Milly Minor");
        dial(minor, "PUBLIC");
        byte[] face = "minor-face".getBytes(StandardCharsets.UTF_8);
        long photo = setPhoto(minor, face);

        mvc.perform(get("/images/" + photo))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + photo)
                        .cookie(completeMember("photo-minor-viewer@example.org", "Adele Adult")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(face));
    }

    /**
     * §3.2, the line this ticket most had to hold: "deliberately not a photo
     * (exclusionary; generated faces are free)". Asserted against
     * {@link CapabilityService} rather than the UI, because a gate would show up
     * there first — a member with no photo holds everything, and adding one earns
     * nothing at all.
     */
    @Test
    void aPhotoIsNeverAGate() throws Exception {
        Cookie complete = completeMember("photo-gate-complete@example.org", "Cara Complete");
        long completeId = memberId(complete);
        for (Capability capability : Capability.values()) {
            assertThat(capabilities.may(completeId, capability))
                    .as("a complete profile with NO photo holds %s", capability)
                    .isEqualTo(CapabilityAnswer.YES);
        }

        setPhoto(complete, "gate-face".getBytes(StandardCharsets.UTF_8));
        for (Capability capability : Capability.values()) {
            assertThat(capabilities.may(completeId, capability))
                    .as("adding a photo changes nothing about %s", capability)
                    .isEqualTo(CapabilityAnswer.YES);
        }

        // And the other direction: a photo buys nothing on an unfinished Profile.
        Cookie faceOnly = signUpAndIn("photo-gate-faceonly@example.org");
        long faceOnlyId = memberId(faceOnly);
        setPhoto(faceOnly, "face-only".getBytes(StandardCharsets.UTF_8));
        for (Capability capability : Capability.values()) {
            assertThat(capabilities.may(faceOnlyId, capability))
                    .as("a photo does not earn %s", capability)
                    .isEqualTo(CapabilityAnswer.NOT_YET_EARNED);
        }
    }

    /** §7.3: a Block is total and symmetric — including at each other's faces. */
    @Test
    void aBlockedPairCannotFetchEachOthersPhotos() throws Exception {
        Cookie blocker = completeMember("photo-block-blocker@example.org", "Bea Blocker");
        Cookie blocked = completeMember("photo-block-blocked@example.org", "Barry Blocked");
        long blockersPhoto = setPhoto(blocker, "blockers-face".getBytes(StandardCharsets.UTF_8));
        long blockedsPhoto = setPhoto(blocked, "blockeds-face".getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/images/" + blockersPhoto).cookie(blocked))
                .andExpect(status().isOk());
        mvc.perform(get("/images/" + blockedsPhoto).cookie(blocker))
                .andExpect(status().isOk());

        mvc.perform(post("/p/" + memberId(blocked) + "/block").cookie(blocker))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/images/" + blockersPhoto).cookie(blocked))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + blockedsPhoto).cookie(blocker))
                .andExpect(status().isNotFound());
        mvc.perform(get("/images/" + blockersPhoto).cookie(blocker))
                .andExpect(status().isOk());
    }

    /**
     * The cache decision #51 set the rule for, applied to faces. A public, complete,
     * adult Profile's photo IS served to the open web — and still never with a shared
     * cache entry. {@code OPEN_WEB} is reserved for images that are public
     * permanently (a Company logo, which no Dial governs); this one's audience is
     * derived from a setting its owner can turn down in the next second (ADR-0002),
     * and a proxy holding it for a year would outlive the change.
     */
    @Test
    void aPublicPhotoIsServedToTheOpenWebAndStillNeverSharedCached() throws Exception {
        Cookie member = completeMember("photo-cache@example.org", "Cass Cache");
        dial(member, "PUBLIC");
        byte[] face = "public-face".getBytes(StandardCharsets.UTF_8);
        long photo = setPhoto(member, face);

        String cacheControl = mvc.perform(get("/images/" + photo))
                .andExpect(status().isOk())
                .andExpect(content().bytes(face))
                .andReturn().getResponse().getHeader("Cache-Control");
        assertThat(cacheControl).contains("no-store").contains("private").doesNotContain("public");
    }

    /** §10.4's cap and type check, on this path like every other — before storage. */
    @Test
    void aPhotoIsTypedAndCappedBeforeItReachesTheProfile() throws Exception {
        Cookie member = completeMember("photo-caps@example.org", "Cap Ped");
        long kept = setPhoto(member, "kept-face".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/profile/photo")
                        .file(new MockMultipartFile("photo", "me.gif", "image/gif", new byte[16]))
                        .cookie(member))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("a PNG or a JPEG")));
        mvc.perform(multipart("/profile/photo")
                        .file(new MockMultipartFile("photo", "huge.png", "image/png",
                                new byte[512 * 1024 + 1]))
                        .cookie(member))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("512 KB")));

        assertThat(storedPhotoId(member)).isEqualTo(kept);
    }

    /**
     * The edit page's own promise (§4.1): removing is a button that is simply there,
     * beside the photo, whenever there is one to remove — and gone when there isn't,
     * because there is nothing to undo.
     */
    @Test
    void theEditPageOffersRemovalOnlyWhenThereIsAPhoto() throws Exception {
        Cookie member = completeMember("photo-editform@example.org", "Edie Form");

        mvc.perform(get("/profile/edit").cookie(member))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Remove photo"))));

        setPhoto(member, "editform-face".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/profile/edit").cookie(member))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Remove photo")));
    }
}
