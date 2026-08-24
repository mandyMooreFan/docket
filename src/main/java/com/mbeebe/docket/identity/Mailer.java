package com.mbeebe.docket.identity;

/**
 * The one outbound-mail port. Plain SMTP behind it, never a vendor SDK (§14.2) —
 * switching provider is configuration, not a rewrite.
 */
public interface Mailer {

    void send(String to, String subject, String body);
}
