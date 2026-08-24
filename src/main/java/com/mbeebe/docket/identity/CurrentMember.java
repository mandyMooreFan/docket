package com.mbeebe.docket.identity;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/** The signed-in Member for this request, resolved once by the interceptor. */
public final class CurrentMember {

    static final String ATTRIBUTE = CurrentMember.class.getName();

    private CurrentMember() {
    }

    public static Optional<Member> get(HttpServletRequest request) {
        return Optional.ofNullable((Member) request.getAttribute(ATTRIBUTE));
    }
}
