package com.mbeebe.docket.profile;

/**
 * A Member as lists point at them — name, initials, photo, address. Never the entity.
 *
 * <p>{@code photoImageId} is an address, not bytes and not a permission: whether this
 * viewer may actually have the picture is decided at /images/{id} by
 * {@link ProfilePhotoAudience}, per request, from the Dial (§8.5). A card whose photo
 * is out of reach simply falls back to {@code initials}, which every card always
 * carries — a Profile with no photo is permanent and entirely fine (§3.2).
 */
public record PersonCard(long memberId, String name, String initials, Long photoImageId,
                         boolean former) {

    public boolean named() {
        return !name.isBlank() && !former;
    }

    public boolean hasPhoto() {
        return photoImageId != null && !former;
    }

    /**
     * The §3.3 shape for a member who has not written a name yet — and §11.2's
     * attribution for one who has left.
     *
     * <p>"A former member" is the whole of what a terminated Member renders as,
     * everywhere a person is drawn: on the Recommendation they wrote, on the Reply
     * they left under someone's Post, on their side of a Thread the other party
     * still reads. §11.2 requires the attribution to survive without the person
     * doing so, and this record is the one place every surface asks — so the
     * wording cannot drift between them, and no template has to remember the rule.
     *
     * <p>Not a link either, wherever a template asks: a former member's Profile
     * 404s, and a name pointing at a 404 is worse than a name that is simply not a
     * link. {@link #named()} and {@link #hasPhoto()} both go false with it, so a
     * card that has been through Termination cannot accidentally still be drawing
     * a face or claiming a name from a row that has gone.
     */
    public String displayName() {
        if (former) {
            return "A former member";
        }
        return named() ? name : "A member";
    }
}
