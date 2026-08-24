package com.mbeebe.docket.feed;

import com.mbeebe.docket.text.Prose;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Posts are plain text (§5.2.1): escaping and linkifying live in the shared
 * {@link Prose} (messaging renders member text with the same guarantee, §7.2);
 * what stays here is the feed's own shape — the link preview, which is a title
 * and a domain, never a large auto-fetched card.
 */
final class PostBodies {

    private PostBodies() {
    }

    /** Escaped paragraphs with linkified URLs — the only HTML a Post can carry. */
    static String toHtml(String body) {
        return Prose.toHtml(body);
    }

    /** §5.2.1: previews are a domain each, never a fetched card; at most three. */
    static List<LinkPreview> previews(String body) {
        List<LinkPreview> previews = new ArrayList<>();
        for (String url : Prose.urls(body, 3)) {
            domainOf(url).ifPresent(domain -> previews.add(new LinkPreview(url, domain)));
        }
        return previews;
    }

    /** The dated-list form of a Post (§5.4): its opening words, one line. */
    static String excerpt(String body) {
        return Prose.excerpt(body);
    }

    private static java.util.Optional<String> domainOf(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null || host.isBlank()) {
                return java.util.Optional.empty();
            }
            String domain = host.toLowerCase(Locale.ROOT);
            return java.util.Optional.of(
                    domain.startsWith("www.") ? domain.substring(4) : domain);
        } catch (IllegalArgumentException invalid) {
            return java.util.Optional.empty();
        }
    }
}
