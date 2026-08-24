package com.mbeebe.docket.leaving;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The one-button export (SPEC.md §11.1, {@code docs/data-rights.md}).
 *
 * <p>The thing under test is not "a file comes back" — it is that one press covers
 * both rights a member is actually exercising: Article 20's portable copy of what
 * they provided, and Article 15's wider access, which reaches other people's words
 * about them and obliges the supplementary information. So the assertions are
 * about what is <em>in</em> the archive, entry by entry, rather than about the
 * mechanics of zipping.
 */
class ExportTests extends LeavingTestBase {

    /**
     * The whole archive against one well-populated member, in one pass, because the
     * failure this guards against is a section quietly going missing — and a
     * section going missing is invisible in any test that only looks at one
     * section. Everything §11.1 names is asserted here by its own content.
     */
    @Test
    void theArchiveCarriesEveryKindOfStoredFactAboutTheMember() throws Exception {
        Cookie member = completeMember("exp-all@example.org", "Ada Archive");
        Cookie friend = completeMember("exp-all-friend@example.org", "Fern Friend");
        connect(friend, member);

        mvc.perform(post("/profile/basics").cookie(member)
                        .param("name", "Ada Archive").param("headline", "Bridge builder")
                        .param("location", "Sheffield")
                        .param("summary", "I put things where people can find them."))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/education").cookie(member)
                        .param("institution", "Archive Polytechnic").param("course", "Records")
                        .param("startYear", "2011").param("endYear", "2014"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/skills").cookie(member).param("name", "Cataloguing"))
                .andExpect(status().is3xxRedirection());
        byte[] face = "exp-all-face".getBytes(StandardCharsets.UTF_8);
        long photo = setPhoto(member, face);

        long post = compose(member, "A post that must survive into the archive.");
        mvc.perform(post("/posts/" + post + "/save").cookie(member))
                .andExpect(status().is3xxRedirection());
        long theirPost = compose(friend, "A thread that Fern started.");
        mvc.perform(post("/posts/" + theirPost + "/replies").cookie(member)
                        .param("body", "A reply left in a thread that was not mine."))
                .andExpect(status().is3xxRedirection());

        recommend(friend, member, "She is unusually good at finding things.");
        recommend(member, friend, "He is unusually good at losing them.");

        Cookie poster = posterAt("exp-all-poster@example.org", "Pat Poster",
                "Exportly", "exportly-all.example");
        long job = postJob(poster, companies.named("Exportly").id(), "Archivist");
        mvc.perform(post("/jobs/" + job + "/apply").cookie(member)
                        .param("note", "I would like to do this job."))
                .andExpect(status().is3xxRedirection());
        String queue = mvc.perform(get("/jobs/" + job + "/applications").cookie(poster))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var applicationId = java.util.regex.Pattern
                .compile("/jobs/" + job + "/applications/(\\d+)/advance").matcher(queue);
        assertThat(applicationId.find()).as("the poster's queue names the application").isTrue();
        mvc.perform(post("/jobs/" + job + "/applications/" + applicationId.group(1) + "/advance")
                        .cookie(poster))
                .andExpect(status().is3xxRedirection());

        send(member, memberId(friend), "I wrote this half.");
        send(friend, memberId(member), "And I wrote this one.");

        Map<String, byte[]> archive = archiveOf(member);

        assertThat(archive.keySet()).contains("README.txt", "your-docket.html",
                "your-messages.html", "about-this-copy.html", "docket-export.json");

        String own = text(archive, "your-docket.html");
        assertThat(own)
                .as("the profile, as stored")
                .contains("Ada Archive").contains("Bridge builder").contains("Sheffield")
                .contains("I put things where people can find them.")
                .as("positions, education and skills")
                .contains("A role").contains("Archive Polytechnic").contains("Cataloguing")
                .as("posts, replies and the private saves")
                .contains("A post that must survive into the archive.")
                .contains("A reply left in a thread that was not mine.")
                .contains("Posts you saved")
                .as("the connection")
                .contains("Fern Friend")
                .as("§11.1: recommendations written BY and ABOUT you, both")
                .contains("He is unusually good at losing them.")
                .contains("She is unusually good at finding things.")
                .as("§11.1: the Application AND the Outcome the poster recorded")
                .contains("Archivist").contains("I would like to do this job.")
                .contains("Advanced");

        String messages = text(archive, "your-messages.html");
        assertThat(messages)
                .as("§11.1: Threads whole, both halves")
                .contains("I wrote this half.").contains("And I wrote this one.");

        assertThat(archive.keySet())
                .as("the member's own photo bytes travel with the archive")
                .contains("media/photo-" + photo + ".png");
        assertThat(archive.get("media/photo-" + photo + ".png")).isEqualTo(face);

        assertThat(text(archive, "docket-export.json"))
                .as("the same facts again, for a machine")
                .contains("\"docket_export\"")
                .contains("A post that must survive into the archive.")
                .contains("And I wrote this one.")
                .contains("\"supplementary_information\"");
    }

    /**
     * §11.1's note, which is spec copy and not a UI detail. It has to be somewhere
     * a person actually reads, so it is asserted in all three places it is put:
     * the plain-text README anyone gets on unzipping, the top of the file the other
     * person's words are in, and above the section itself.
     */
    @Test
    void theCorrespondenceNoteIsWhereAPersonWillReadIt() throws Exception {
        Cookie member = completeMember("exp-note@example.org", "Nora Note");
        Cookie other = completeMember("exp-note-other@example.org", "Otto Other");
        connect(other, member);
        send(other, memberId(member), "Something confidential between us.");

        Map<String, byte[]> archive = archiveOf(member);
        // Plain text keeps the apostrophe; a rendered document escapes it, so the
        // HTML is asserted on the half of the sentence that survives either way.
        String note = ExportSection.CORRESPONDENCE_NOTE;
        String noteInHtml = "words are in here. They are yours to keep, not to reuse.";

        assertThat(text(archive, "README.txt"))
                .as("the first thing anyone sees on unzipping")
                .contains(note);
        String messages = text(archive, "your-messages.html");
        assertThat(messages).contains(noteInHtml);
        assertThat(messages.indexOf(noteInHtml))
                .as("the note stands above the other person's words, never below them")
                .isLessThan(messages.indexOf("Something confidential between us."));
        assertThat(text(archive, "docket-export.json")).contains(note);
    }

    /**
     * §11.1's Article 15 half: the supplementary information is required — purposes,
     * recipients, retention, source — and it goes in the same archive because
     * "members don't know the difference and shouldn't have to".
     */
    @Test
    void theArchiveCarriesTheArticleFifteenSupplementaryInformation() throws Exception {
        Cookie member = completeMember("exp-supp@example.org", "Sam Supplement");

        String about = text(archiveOf(member), "about-this-copy.html");
        assertThat(about)
                .contains("What we use your data for")
                .contains("Who else sees it")
                .contains("How long we keep it")
                .contains("Where it came from")
                .as("§11.3's backups position, in the archive as well as in the app")
                .contains("beyond use");
    }

    /**
     * ADR-0002 and §11.1: derived Capabilities and effective visibility fall outside
     * the right as a category, because they are never stored. The requirement is not
     * that they are absent — it is that the archive <em>says</em> they are absent
     * and why, rather than leaving a member to notice a gap and read it as
     * concealment.
     */
    @Test
    void theArchiveSaysWhatIsNotInItAndWhyRatherThanOmittingItQuietly() throws Exception {
        Cookie member = completeMember("exp-derived@example.org", "Della Derived");

        String about = text(archiveOf(member), "about-this-copy.html");
        assertThat(about)
                .contains("What is not in here")
                .contains("does not store conclusions")
                .contains("worked out fresh every time")
                .as("§15.2's ⚠️: the lawful bases are undetermined, and the copy says so")
                .contains("has not yet finished determining which");
    }

    /**
     * The §51 guard, at the one route that could have been a hole in it.
     *
     * <p>There is no member id in the URL, so "somebody else's archive" is not an
     * address that exists: a second member asking gets their own, containing none
     * of the first member's private words. Signed out is a plain 404 — the
     * discipline the rest of the product applies to anything not yours to see.
     */
    @Test
    void theDownloadRouteServesOnlyTheRequestingMember() throws Exception {
        Cookie first = completeMember("exp-scope-first@example.org", "Fiona First");
        Cookie second = completeMember("exp-scope-second@example.org", "Seth Second");
        long firstPost = compose(first, "A post that belongs to Fiona alone.");
        mvc.perform(post("/posts/" + firstPost + "/save").cookie(first))
                .andExpect(status().is3xxRedirection());
        compose(second, "A post that belongs to Seth alone.");

        mvc.perform(get("/settings/data/export.zip")).andExpect(status().isNotFound());

        assertThat(allDocuments(archiveOf(second)))
                .as("Seth asking gets Seth")
                .contains("Seth Second").contains("A post that belongs to Seth alone.")
                .as("and the route is not a second door onto Fiona")
                .doesNotContain("A post that belongs to Fiona alone.")
                .doesNotContain("Fiona First");
        assertThat(allDocuments(archiveOf(first)))
                .as("and Fiona asking gets Fiona")
                .contains("A post that belongs to Fiona alone.")
                .doesNotContain("A post that belongs to Seth alone.");
    }

    /**
     * A message image is the sharpest case for the archive-versus-guard question:
     * bytes that /images/{id} refuses to everybody but the two people in the
     * Thread. They belong in both participants' archives, and in nobody else's —
     * which is exactly what falls out of fetching through the guard with the
     * member as the viewer, rather than round it.
     */
    @Test
    void imagesInTheArchiveComeThroughTheSameAudienceGuardAsTheirUrl() throws Exception {
        Cookie member = completeMember("exp-img@example.org", "Ivy Image");
        Cookie other = completeMember("exp-img-other@example.org", "Owen Other");
        Cookie stranger = completeMember("exp-img-stranger@example.org", "Stan Stranger");
        connect(other, member);

        byte[] picture = "exp-img-in-a-message".getBytes(StandardCharsets.UTF_8);
        mvc.perform(multipart("/messages/" + memberId(other)).cookie(member)
                        .file(new MockMultipartFile("images", "note.png", "image/png", picture))
                        .param("body", "Here is a picture."))
                .andExpect(status().is3xxRedirection());

        String thread = threadPage(member, memberId(other));
        var matcher = java.util.regex.Pattern.compile("/images/(\\d+)").matcher(thread);
        assertThat(matcher.find()).as("the thread renders the image").isTrue();
        String entry = "media/message-" + matcher.group(1) + ".png";

        assertThat(archiveOf(member)).containsKey(entry);
        assertThat(archiveOf(member).get(entry)).isEqualTo(picture);
        assertThat(archiveOf(other))
                .as("the other half of the correspondence gets it too")
                .containsKey(entry);
        assertThat(archiveOf(stranger))
                .as("and nobody else does — the archive is the guard, not a way past it")
                .doesNotContainKey(entry);
    }

    /**
     * The one part of the hand-rolled JSON that has to be right (see
     * {@code leaving.Json}): member-written text reaches it unaltered, and an
     * archive that does not parse is the one failure a portable copy cannot have.
     */
    @Test
    void memberWrittenTextIsEscapedSoTheJsonStillParses() throws Exception {
        Cookie member = completeMember("exp-json@example.org", "Quinn Quote");
        compose(member, "She said \"hello\" and\nthen \\ left.");

        String json = text(archiveOf(member), "docket-export.json");
        assertThat(json)
                .contains("She said \\\"hello\\\" and\\nthen \\\\ left.")
                .as("the raw characters never reach the document unescaped")
                .doesNotContain("said \"hello\" and");
    }
}
