package com.mbeebe.docket.messaging;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * One Message as one reader sees it (§7.2). The body is escaped-and-linkified
 * HTML from the shared {@link com.mbeebe.docket.text.Prose}, rendered serif
 * because a person wrote it (§2); everything around it is app-said and sans.
 *
 * <p>Nothing here reports on anyone: there is no "read", no "seen", no
 * last-seen. {@code id} is the stable handle a Report will point at (§10.2,
 * #38) — Messages in a Thread you are part of are reportable, so no rendering
 * may leave a Message row unreachable.
 */
public record MessageView(long id, PersonCard author, boolean mine, String when,
                          String bodyHtml, List<Long> imageIds) {
}
