package com.mbeebe.docket.moderation;

/**
 * One open intimate image report as the queue shows it (§10.5).
 *
 * <p>{@code held} is the fact the reviewer most needs: content named precisely enough
 * was hidden when the report arrived, and content that was not is still visible and
 * waiting on them. The two cases need different urgency and the queue should not make
 * them look alike.
 */
public record IntimateImageView(long id, String locator, boolean held) {
}
