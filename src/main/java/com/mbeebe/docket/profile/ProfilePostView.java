package com.mbeebe.docket.profile;

/** One line of the §5.4 dated Posts list on a Profile — an excerpt, never the entity. */
public record ProfilePostView(long postId, String when, String excerpt) {
}
