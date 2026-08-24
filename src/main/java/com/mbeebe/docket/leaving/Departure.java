package com.mbeebe.docket.leaving;

/**
 * What an owning module does with its own rows when a Member leaves (§11.2).
 *
 * <p>{@code ExportContributor}'s twin, and deliberately its mirror image: the
 * module that knows what it stores is the module that decides what happens to it,
 * and each answer is a paragraph of §11.2 argued where the rows live rather than
 * a line in one central deleter that nobody would read as a rule.
 *
 * <p>The rule being applied is one sentence with two halves — <em>your Profile
 * goes; anything that stood alone is unpublished</em>, and <em>your side of a
 * Thread stays, attributed to a former member; Recommendations you wrote stay
 * published</em>. The dividing line is not "yours" versus "not yours": everything
 * here was written by the departing member. It is whether the thing stood on its
 * own — a Profile, a Post — or was said inside somebody else's record, where
 * removing it would leave a hole in a page or a conversation that is not the
 * leaver's to edit. §11.2 states the accepted cost outright: you cannot fully
 * disappear from Docket, and the alternative is worse.
 *
 * <p>Contributors run in {@code @Order} sequence, before identity writes the
 * tombstone — until it does, the Member is still findable, which several of these
 * need.
 */
public interface Departure {

    /** Take this module's rows out, keep what §11.2 says stays. Idempotent. */
    void memberLeaving(long memberId);
}
