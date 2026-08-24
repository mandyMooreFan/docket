package com.mbeebe.docket;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock tests can push forward — expiry, rate windows and the 90-day slide need one. */
public class SteppingClock extends Clock {

    private volatile Instant instant = Instant.now();
    private final ZoneId zone = ZoneId.systemDefault();

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
