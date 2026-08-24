package com.mbeebe.docket.feed;

/**
 * §5.2.1: a link preview is a title and a domain, never a large auto-fetched
 * card. v1 renders the domain only — fetching titles server-side is an SSRF
 * surface out of proportion to the feature, recorded as deliberately skipped.
 */
public record LinkPreview(String url, String domain) {
}
