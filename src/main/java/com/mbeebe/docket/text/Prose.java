package com.mbeebe.docket.text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Member-written plain text rendered safely (SPEC.md §5.2.1, §7.2): everything
 * a member wrote is escaped wholesale, then URLs — and only URLs — become safe
 * anchors. Extracted from the feed's PostBodies when Messages needed the same
 * guarantee; there is deliberately no server-side URL fetching anywhere (no
 * title, no card, no SSRF surface).
 */
public final class Prose {

    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+");
    private static final String TRAILING_PUNCTUATION = ".,;:!?)";
    private static final int EXCERPT_LENGTH = 180;

    private Prose() {
    }

    /** Escaped paragraphs with linkified URLs — the only HTML member text can carry. */
    public static String toHtml(String body) {
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

    /** The one-line form of a longer text: its opening words. */
    public static String excerpt(String body) {
        String flat = body.replaceAll("\\s+", " ").strip();
        return flat.length() <= EXCERPT_LENGTH
                ? flat
                : flat.substring(0, EXCERPT_LENGTH - 1).stripTrailing() + "…";
    }

    /** The first distinct URLs in the text, trailing punctuation trimmed. */
    public static List<String> urls(String body, int limit) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher matcher = URL.matcher(body);
        while (matcher.find() && found.size() < limit) {
            found.add(trimTrailingPunctuation(matcher.group()));
        }
        return new ArrayList<>(found);
    }

    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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
}
