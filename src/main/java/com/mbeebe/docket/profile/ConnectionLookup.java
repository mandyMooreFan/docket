package com.mbeebe.docket.profile;

/**
 * The one question visibility asks the graph: are these two Members connected?
 * The graph itself is a later build increment (Build Connections, #32), which
 * replaces {@link NoConnectionsYet} with the real answer.
 */
public interface ConnectionLookup {

    boolean connected(long memberA, long memberB);
}
