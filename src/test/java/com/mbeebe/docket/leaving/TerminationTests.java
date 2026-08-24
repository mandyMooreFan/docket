package com.mbeebe.docket.leaving;

import com.mbeebe.docket.identity.Members;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Termination (SPEC.md §11.2).
 *
 * <p>The half of §11.2 worth testing is not the deleting — it is the keeping. "Your
 * side of a Thread stays, attributed to a former member; Recommendations you wrote
 * stay published" is the accepted cost the spec states outright, and it is the
 * thing a later refactor is most likely to break by helpfully adding a cascade.
 * Every assertion below is scoped to the members it names.
 */
class TerminationTests extends LeavingTestBase {

    @Autowired
    Termination termination;

    @Autowired
    Members members;

    /**
     * §11.2: "Deletion offers the export first and never requires it."
     *
     * <p>Both halves. The offer is on the page, above the confirmation; and the
     * deletion goes through for a member who never once touched the export route —
     * which is what stops the offer becoming a toll gate.
     */
    @Test
    void terminationOffersTheExportFirstAndDoesNotRequireIt() throws Exception {
        Cookie member = completeMember("term-offer@example.org", "Olive Offer");

        String page = mvc.perform(get("/settings/data").cookie(member))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("/settings/data/export.zip").contains("Download my data");
        assertThat(page.indexOf("/settings/data/export.zip"))
                .as("the offer stands above the door out, not beside it")
                .isLessThan(page.indexOf("/settings/data/leave"));
        assertThat(flat(page)).contains("You do not have to download anything first.");

        // Never exported. Leaving works anyway.
        leave(member);
        mvc.perform(get("/profile").cookie(member))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * §11.2's first clause and the §11.3 copy that has to be honest about backups —
     * asserted on the page a member reads before pressing the button, because that
     * is where the spec puts it.
     */
    @Test
    void theDeletionCopyStatesTheBackupsPositionAndTheComplaintsRoutePlainly()
            throws Exception {
        Cookie member = completeMember("term-copy@example.org", "Cora Copy");

        String page = mvc.perform(get("/settings/data").cookie(member))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(flat(page))
                .as("§11.3: put beyond use until the backups roll, said plainly")
                .contains("put beyond use")
                .contains("roll over on their normal schedule")
                .as("§11.3: a data-protection complaint route, with its 30-day clock")
                .contains("acknowledge it within 30 days")
                .contains("Information Commissioner")
                .as("§11.2's accepted cost, not hidden")
                .contains("you cannot completely disappear from Docket");

        leave(member);
        assertThat(flat(mvc.perform(get("/left")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()))
                .as("and again on the last page they can still reach")
                .contains("put beyond use");
    }

    /**
     * §11.2's "your Profile goes; anything that stood alone is unpublished",
     * checked at all three surfaces the Profile feeds: its own page, the feed, and
     * search — because each derives from it separately and a fix in one is not a
     * fix in the others.
     */
    @Test
    void afterLeavingTheProfileIsGoneFromItsPageFromFeedsAndFromSearch() throws Exception {
        Cookie leaver = completeMember("term-gone@example.org", "Gwen Gonzaloq");
        Cookie watcher = completeMember("term-gone-watcher@example.org", "Wes Watcher");
        connect(watcher, leaver);
        long leaverId = memberId(leaver);
        compose(leaver, "A post by Gwen Gonzaloq that must not outlive her account.");

        assertThat(feedSeenBy(watcher)).contains("A post by Gwen Gonzaloq");
        assertThat(mvc.perform(get("/search").param("q", "Gonzaloq").cookie(watcher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .contains("Gwen Gonzaloq");

        leave(leaver);

        mvc.perform(get("/p/" + leaverId).cookie(watcher)).andExpect(status().isNotFound());
        mvc.perform(get("/p/" + leaverId)).andExpect(status().isNotFound());
        assertThat(feedSeenBy(watcher)).doesNotContain("A post by Gwen Gonzaloq");
        assertThat(mvc.perform(get("/search").param("q", "Gonzaloq").cookie(watcher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .doesNotContain("Gwen Gonzaloq");
    }

    /**
     * The clause the non-cascading foreign keys in V8 exist for. Both halves of the
     * Thread are still there for the person who stayed, and the person who left is
     * attributed rather than erased — §11.2's alternative was "delete my account"
     * deleting a colleague's correspondence into a monologue of holes.
     */
    @Test
    void theirSideOfEachThreadStaysReadableAttributedToAFormerMember() throws Exception {
        Cookie leaver = completeMember("term-thread-leaver@example.org", "Lena Leaver");
        Cookie stayer = completeMember("term-thread-stayer@example.org", "Sid Stayer");
        connect(stayer, leaver);
        long leaverId = memberId(leaver);
        send(leaver, memberId(stayer), "This is the half Lena wrote.");
        send(stayer, leaverId, "And this is the half Sid wrote.");

        leave(leaver);

        String thread = threadPage(stayer, leaverId);
        assertThat(thread)
                .as("neither person may destroy the other's record (§7.3, §11.1)")
                .contains("This is the half Lena wrote.")
                .contains("And this is the half Sid wrote.")
                .as("§11.2: attributed to a former member")
                .contains("A former member")
                .doesNotContain("Lena Leaver")
                .as("named, but not linked — the page behind that name is a 404")
                .doesNotContain("href=\"/p/" + leaverId + "\"");
        assertThat(inboxPage(stayer))
                .as("the correspondence stays listed, under the same attribution")
                .contains("A former member").doesNotContain("Lena Leaver");
    }

    /** §11.2: "Recommendations you wrote stay published." */
    @Test
    void recommendationsTheyWroteStayPublished() throws Exception {
        Cookie author = completeMember("term-rec-author@example.org", "Ruth Recommender");
        Cookie subject = completeMember("term-rec-subject@example.org", "Sol Subject");
        connect(author, subject);
        recommend(author, subject, "Sol turned a mess into a plan in an afternoon.");

        long subjectId = memberId(subject);
        assertThat(profilePage(subject, subjectId))
                .contains("Sol turned a mess into a plan in an afternoon.")
                .contains("Ruth Recommender");

        leave(author);

        String page = profilePage(subject, subjectId);
        assertThat(page)
                .as("the words stay up — they are primarily the subject's data")
                .contains("Sol turned a mess into a plan in an afternoon.")
                .as("de-identified attribution, not removal (EDPB 01/2022 ¶173)")
                .contains("A former member")
                .doesNotContain("Ruth Recommender");
    }

    /**
     * #52's resolution, verified rather than trusted: a photo nobody claims is
     * served to nobody, by {@code ImageAudiences}' fail-closed default. Termination
     * takes the Profile row that claimed it, so the same rule a replaced photo
     * already lived under does the work.
     */
    @Test
    void theirPhotoBecomesUnreachable() throws Exception {
        Cookie leaver = completeMember("term-photo@example.org", "Phoebe Photo");
        Cookie viewer = completeMember("term-photo-viewer@example.org", "Val Viewer");
        long photo = setPhoto(leaver, "term-photo-face".getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/images/" + photo).cookie(viewer)).andExpect(status().isOk());

        leave(leaver);

        mvc.perform(get("/images/" + photo).cookie(viewer)).andExpect(status().isNotFound());
        mvc.perform(get("/images/" + photo)).andExpect(status().isNotFound());
    }

    /**
     * §9.4, at the one moment it could quietly fail: leaving must not make anything
     * <em>more</em> visible than it was. A minor's Reply survives Termination (it
     * never stood alone), and the authored-as-minor fact that caps it lives on the
     * Reply row rather than on the Member — which is exactly why §9.3 could delete
     * the birth data and the cap still holds over a tombstone.
     */
    @Test
    void aTerminatedMinorsContentStaysCapped() throws Exception {
        Cookie minor = completeMinor("term-minor@example.org", "Mimi Minor");
        Cookie adult = completeMember("term-minor-adult@example.org", "Alan Adult");
        connect(minor, adult);
        mvc.perform(post("/profile/dial").cookie(adult).param("dial", "PUBLIC"))
                .andExpect(status().is3xxRedirection());
        long post = compose(adult, "A public post, open to the web.");
        mvc.perform(post("/posts/" + post + "/replies").cookie(minor)
                        .param("body", "A reply written before she turned eighteen."))
                .andExpect(status().is3xxRedirection());

        String signedIn = mvc.perform(get("/posts/" + post).cookie(adult))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(signedIn).contains("A reply written before she turned eighteen.");
        String loggedOut = mvc.perform(get("/posts/" + post))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(loggedOut).doesNotContain("A reply written before she turned eighteen.");

        leave(minor);

        assertThat(mvc.perform(get("/posts/" + post).cookie(adult))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .as("§11.2: it did not stand alone, so it stays — attributed")
                .contains("A reply written before she turned eighteen.")
                .contains("A former member").doesNotContain("Mimi Minor");
        assertThat(mvc.perform(get("/posts/" + post))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .as("§9.4: and the cap is exactly where it was — leaving lifts nothing")
                .doesNotContain("A reply written before she turned eighteen.");
    }

    /**
     * The account itself is over: every session destroyed, the address released, and
     * nothing left that could sign anyone back in. The old address joining again
     * gets a new Member, not the tombstone.
     */
    @Test
    void theAccountIsOverAndTheAddressIsFree() throws Exception {
        Cookie leaver = completeMember("term-account@example.org", "Tam Tombstone");
        long oldId = memberId(leaver);

        leave(leaver);

        mvc.perform(get("/settings/data").cookie(leaver))
                .andExpect(status().is3xxRedirection());
        assertThat(members.find(oldId)).isPresent();
        assertThat(members.find(oldId).orElseThrow().terminated()).isTrue();
        assertThat(members.find(oldId).orElseThrow().email())
                .as("nothing on the tombstone identifies the person")
                .doesNotContain("term-account@example.org");

        Cookie rejoined = signUpAndIn("term-account@example.org");
        assertThat(memberId(rejoined))
                .as("joining again is a new account, from nothing")
                .isNotEqualTo(oldId);
    }

    /**
     * The seam #38 asked for. The §10.3 ladder's fourth rung and §11.2's door are
     * the same primitive with a different {@link Termination.Reason}, and calling it
     * from moderation has to leave a member in exactly the state §11.2 describes —
     * including the parts a ladder would never think about, like the Thread staying
     * readable for the other party.
     */
    @Test
    void theTerminationPortCarriesTheSameSemanticsForModerationAndIsIdempotent()
            throws Exception {
        Cookie removed = completeMember("term-port@example.org", "Mal Moderated");
        Cookie other = completeMember("term-port-other@example.org", "Ola Other");
        connect(other, removed);
        long removedId = memberId(removed);
        send(removed, memberId(other), "Something Mal said before the ladder reached him.");

        var member = members.find(removedId).orElseThrow();
        termination.terminate(member, Termination.Reason.MODERATION);
        // A second pass — a double-submitted form, or a ladder racing a member's own
        // departure — must do nothing at all.
        termination.terminate(members.find(removedId).orElseThrow(),
                Termination.Reason.MODERATION);

        mvc.perform(get("/p/" + removedId).cookie(other)).andExpect(status().isNotFound());
        assertThat(threadPage(other, removedId))
                .contains("Something Mal said before the ladder reached him.")
                .contains("A former member");
    }
}
