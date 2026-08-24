package com.mbeebe.docket.identity;

import java.time.YearMonth;

/**
 * SPEC.md §9.3 stores only month + year, so a birthday is never knowable to the day.
 * One rule covers both ends: a person is treated as N years old only after the END of
 * their birth month — the same boundary the 18 rollover uses ("the end of the birth
 * month they turn 18"). Conservative in both directions, and consistent.
 */
final class AgeRules {

    private AgeRules() {
    }

    static boolean reached(int years, YearMonth birth, YearMonth now) {
        return now.isAfter(birth.plusYears(years));
    }
}
