package com.mbeebe.docket.profile;

/**
 * The two questions visibility asks the graph (built by #32): are these two
 * Members connected, and does a Block stand between them? Both symmetric.
 */
public interface ConnectionLookup {

    boolean connected(long memberA, long memberB);

    /** A Block in either direction (§7.3): total, both ways, so a single question. */
    boolean blocked(long memberA, long memberB);
}
