package com.mbeebe.docket.moderation;

/** One open data-protection complaint as the queue shows it (§11.3). */
public record ComplaintView(long id, String contact, String account) {
}
