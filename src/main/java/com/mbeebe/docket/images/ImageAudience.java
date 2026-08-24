package com.mbeebe.docket.images;

import com.mbeebe.docket.identity.Member;

import java.util.Optional;

/**
 * How an owning module answers for the images it put in the one store (§10.4).
 *
 * <p>Stored bytes carry no visibility of their own — visibility belongs to the thing
 * the image is <em>on</em>: a Post rides its author's Dial (§5.4), a Company logo is
 * on the open web because the Company page is (§6.1, §8.4). ADR-0002 says that
 * derivation is made at read time and never stored, so /images/{id} cannot read a
 * flag off the row; it has to ask the owner, every request, with the viewer in hand.
 * Hence the inverted dependency, the same shape as {@code profile.ConnectionLookup}:
 * the images module states the question, each owning module contributes the answer
 * for its own kind, and nothing about Posts or Companies leaks into this package.
 *
 * <p>Contributors claim disjointly — an image belongs to exactly one thing — and an
 * image nobody claims is served to nobody (§8.5: no enumeration surface). Fail-closed
 * is the whole point: the sequential {@code image.id} is trivially walkable, so an
 * unclaimed row must be a wall, not a hole.
 */
public interface ImageAudience {

    /** Who a claimed image is for, as far as this contributor's rules go. */
    enum Verdict {

        /**
         * §8.4: on the open web for everyone, logged-out included, and permanently so —
         * no Dial governs it. Only this verdict may ride a shared cache.
         */
        OPEN_WEB,

        /**
         * This viewer may see it, but by a derivation that can change under them —
         * a Dial turned down, a Block raised. Served, never shared-cached.
         */
        THIS_VIEWER,

        /** Not for this viewer: a plain 404, no placeholder, the Profile page's discipline. */
        HIDDEN
    }

    /**
     * The verdict for this image and this viewer, or empty when this module does not
     * own the image — an unowned image is another contributor's, or nobody's at all.
     *
     * @param viewer the signed-in Member, or empty for the open web
     */
    Optional<Verdict> verdictFor(long imageId, Optional<Member> viewer);
}
