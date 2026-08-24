package com.mbeebe.docket.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a person typed, turned into a Postgres tsquery — the one place raw input
 * becomes a query, shared by every group and by the jobs board's keyword filter.
 *
 * <p>Two decisions live here.
 *
 * <p><b>Prefix matching on every term.</b> {@code Zygg} must reach Zyggurat and
 * {@code Quand} must reach Quandle, because a name search is what people type
 * while they are still typing. So each term becomes {@code term:*}, joined with
 * AND. The alternative — an ILIKE fallback for short queries — would be a second
 * matching rule with its own edge cases and no index behind it.
 *
 * <p><b>Only letters and digits survive.</b> The extraction keeps
 * {@code [\p{L}\p{N}]+} and drops everything else, so {@code *}, {@code %},
 * {@code !}, {@code |}, {@code &}, {@code :} and quotes can never reach
 * to_tsquery. That is not sanitising after the fact: there is no path by which a
 * tsquery operator a member typed becomes a tsquery operator Postgres runs, which
 * is why §8.5's no-enumeration rule cannot be picked open with punctuation.
 *
 * <p>A query carrying fewer than {@link #MIN_CHARACTERS} letters or digits in
 * total is refused outright and searches nothing. Below that a prefix match is a
 * browse of the membership, not a search for someone — and §8.5 says no
 * enumeration surface exists beyond the named ones.
 */
public final class SearchTerms {

    /** Under two characters, a prefix search is a browse. No results, no query run. */
    static final int MIN_CHARACTERS = 2;

    /** A bound on the work one query may ask for; nobody searches nine words. */
    static final int MAX_TERMS = 8;

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private SearchTerms() {
    }

    /**
     * The tsquery for this raw input, or empty when there is nothing to ask —
     * which callers must render as "no results", never as "everything".
     */
    public static Optional<String> prefixQuery(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        List<String> terms = new ArrayList<>();
        int characters = 0;
        Matcher matcher = WORD.matcher(raw);
        while (matcher.find() && terms.size() < MAX_TERMS) {
            String term = matcher.group().toLowerCase(Locale.ROOT);
            characters += term.length();
            terms.add(term + ":*");
        }
        if (terms.isEmpty() || characters < MIN_CHARACTERS) {
            return Optional.empty();
        }
        return Optional.of(String.join(" & ", terms));
    }
}
