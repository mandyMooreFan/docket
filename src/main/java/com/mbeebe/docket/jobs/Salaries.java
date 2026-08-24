package com.mbeebe.docket.jobs;

import java.util.List;
import java.util.Map;

/**
 * The mandatory real salary range (§6.3), rendered one way everywhere — at the
 * top of the posting and in every list row. The currency list is small and
 * curated; growing it is editing this list, not a design change.
 */
final class Salaries {

    static final List<String> CURRENCIES = List.of("GBP", "EUR", "USD", "CAD", "AUD", "CHF");

    private static final Map<String, String> SYMBOLS =
            Map.of("GBP", "£", "EUR", "€", "USD", "$", "CAD", "CA$", "AUD", "A$");

    private Salaries() {
    }

    static boolean known(String currency) {
        return CURRENCIES.contains(currency);
    }

    /** "£45,000–£60,000" — or "CHF 90,000–110,000" where no symbol reads naturally. */
    static String line(int min, int max, String currency) {
        String symbol = SYMBOLS.get(currency);
        return symbol != null
                ? symbol + "%,d".formatted(min) + "–" + symbol + "%,d".formatted(max)
                : currency + " " + "%,d".formatted(min) + "–" + "%,d".formatted(max);
    }
}
