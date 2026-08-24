package com.mbeebe.docket.feed;

import com.mbeebe.docket.profile.PersonCard;

/** One Reply as the template gets it — already filtered for this viewer. */
public record ReplyView(long id, PersonCard author, String when, String bodyHtml) {
}
