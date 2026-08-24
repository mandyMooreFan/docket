package com.mbeebe.docket.profile;

/** One education entry as a template renders it; years is empty when none were given. */
public record EducationView(long id, String institution, String course, String years) {
}
