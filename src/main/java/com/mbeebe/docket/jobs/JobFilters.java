package com.mbeebe.docket.jobs;

/**
 * §6.5's seeker-chosen filters, exactly as the GET params carried them: keyword,
 * location, remote policy, salary floor (a number in one currency — Docket holds
 * no exchange rates, so a floor only ever compares within its own currency),
 * company, and "roles where I know someone" — a fact about the graph, chosen by
 * the seeker, never a weight. Echoed back into the form; a saved search stores
 * this same shape.
 */
public record JobFilters(String q, String location, String remote, String floor,
                         String currency, String company, boolean known) {

    public static JobFilters none() {
        return new JobFilters("", "", "", "", "GBP", "", false);
    }

    public boolean any() {
        return !q.isBlank() || !location.isBlank() || !remote.isBlank()
                || !floor.isBlank() || !company.isBlank() || known;
    }
}
