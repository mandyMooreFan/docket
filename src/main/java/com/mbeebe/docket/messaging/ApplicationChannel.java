package com.mbeebe.docket.messaging;

/**
 * What messaging asks the jobs board (§7.1, ADR-0001): is there an open
 * Application between these two Members, either way round? One question, so
 * that "a Connection or an open Application" stays one gate and not two
 * mechanisms — the same seam shape as
 * {@link com.mbeebe.docket.feed.JobBoardLookup}, implemented in
 * com.mbeebe.docket.jobs.
 *
 * <p>The window an Application authorises is the board's fact to answer, not
 * messaging's to guess: it runs from the moment the Application is made until
 * the Application reaches its ending — "not selected", or the §6.4 close
 * without response. <strong>"Advanced" is not an ending</strong>: it is the
 * beginning of a conversation the poster's Outcome deliberately sends no mail
 * about (§7.4), so the channel stays open for as long as that conversation
 * might run.
 */
public interface ApplicationChannel {

    boolean openBetween(long memberA, long memberB);
}
