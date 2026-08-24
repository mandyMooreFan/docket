package com.mbeebe.docket.moderation;

/**
 * What a Report or a removal points at (§10.2). The complete list of reportable things:
 * Profiles, Posts, Replies, Messages, Job postings and Companies.
 *
 * <p>What is missing is the load-bearing part. There is no kind here for a Thread, and
 * no kind for "a Member" — a Thread you are not part of is not reportable because
 * private is private by construction, and a Member is reached through the Profile they
 * publish. The enum is the enumeration §10.2 asks for, so a route that tried to report
 * something else has nothing to name it with.
 */
public enum TargetKind {

    PROFILE,
    POST,
    REPLY,
    MESSAGE,
    JOB_POSTING,
    COMPANY
}
