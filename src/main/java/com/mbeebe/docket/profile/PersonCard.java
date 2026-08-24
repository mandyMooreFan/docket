package com.mbeebe.docket.profile;

/** A Member as lists point at them — name, initials, address. Never the entity. */
public record PersonCard(long memberId, String name, String initials) {

    public boolean named() {
        return !name.isBlank();
    }

    /** The §3.3 shape for a member who has not written a name yet. */
    public String displayName() {
        return named() ? name : "A member";
    }
}
