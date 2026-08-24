package com.mbeebe.docket.feed;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Posts are plain text (§5.2.1): everything a member wrote is escaped wholesale,
 * then URLs — and only URLs — become safe anchors. The link preview is a domain
 * and nothing more; there is deliberately no server-side URL fetching (no title,
 * no card, no SSRF surface).
 */
final class PostBodies {

    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+");
    private static final String TRAILING_PUNCTUATION = ".,;:!?)";
    private static final int EXCERPT_LENGTH = 180;

    private PostBodies() {
    }

    /** Escaped paragraphs with linkified URLs — the only HTML a Post can carry. */
    static String toHtml(String body) {
        StringBuilder html = new StringBuilder();
        for (String paragraph : body.split("\\n{2,}")) {
            if (paragraph.isBlank()) {
                continue;
            }
            html.append("<p>")
                    .append(linkify(paragraph.strip()).replace("\n", "<br>"))
                    .append("</p>\n");
        }
        return html.toString();
    }

    /** §5.2.1: previews are a domain each, never a fetched card; at most three. */
    static List<LinkPreview> previews(String body) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Matcher matcher = URL.matcher(body);
        while (matcher.find() && urls.size() < 3) {
            urls.add(trimTrailingPunctuation(matcher.group()));
        }
        List<LinkPreview> previews = new ArrayList<>();
        for (String url : urls) {
            domainOf(url).ifPresent(domain -> previews.add(new LinkPreview(url, domain)));
        }
        return previews;
    }

    /** The dated-list form of a Post (§5.4): its opening words, one line. */
    static String excerpt(String body) {
        String flat = body.replaceAll("\\s+", " ").strip();
        return flat.length() <= EXCERPT_LENGTH
                ? flat
                : flat.substring(0, EXCERPT_LENGTH - 1).stripTrailing() + "…";
    }

    private static String linkify(String text) {
        StringBuilder out = new StringBuilder();
        Matcher matcher = URL.matcher(text);
        int last = 0;
        while (matcher.find()) {
            String url = trimTrailingPunctuation(matcher.group());
            out.append(escape(text.substring(last, matcher.start())));
            out.append("<a href=\"").append(escape(url))
                    .append("\" rel=\"nofollow noopener\">").append(escape(url)).append("</a>");
            last = matcher.start() + url.length();
        }
        out.append(escape(text.substring(last)));
        return out.toString();
    }

    private static String trimTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && TRAILING_PUNCTUATION.indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
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

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
