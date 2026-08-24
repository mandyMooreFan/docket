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
public record PersonCard(long memberId, String name, String initials, Long photoImageId) {

    public boolean named() {
        return !name.isBlank();
    }

    public boolean hasPhoto() {
        return photoImageId != null;
    }

    /** The §3.3 shape for a member who has not written a name yet. */
    public String displayName() {
        return named() ? name : "A member";
    }
}
