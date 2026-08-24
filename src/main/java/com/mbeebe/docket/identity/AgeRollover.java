package com.mbeebe.docket.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;

/**
 * §9.3: a 16–17-year-old's birth month/year is kept for exactly one purpose —
 * lifting the protections automatically at the end of the birth month they turn
 * 18 — and is deleted at that rollover, collapsing to the same adult fact.
 */
@Component
public class AgeRollover {

    private static final Logger log = LoggerFactory.getLogger(AgeRollover.class);

    private final MemberRepository members;
    private final Clock clock;

    AgeRollover(MemberRepository members, Clock clock) {
        this.members = members;
        this.clock = clock;
    }

    @Scheduled(cron = "0 20 3 * * *")
    void nightly() {
        int rolled = rolloverDueMinors(YearMonth.now(clock));
        if (rolled > 0) {
            log.info("Age rollover: {} member(s) collapsed to the adult fact", rolled);
        }
    }

    @Transactional
    public int rolloverDueMinors(YearMonth now) {
        int rolled = 0;
        for (Member member : members.findByAgeKind(Member.AgeKind.MINOR)) {
            if (member.dueForRollover(now)) {
                member.rolloverToAdult();
                rolled++;
            }
        }
        return rolled;
    }
}
