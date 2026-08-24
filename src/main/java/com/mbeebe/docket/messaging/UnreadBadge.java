package com.mbeebe.docket.messaging;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * The Unread count on the app bar's Messages item (§7.4) — the only badge in
 * the whole product, and §5.6's single named exception to "no unread badges or
 * dots", earned because a person wrote to you personally rather than because
 * content exists.
 *
 * <p>Null at zero, deliberately: a badge that can render "0" is a badge that
 * will, and §5.6 does not want a zero on the page any more than it wants a dot.
 * Derived on every request from the Messages themselves (ADR-0002) — there is
 * no stored counter to drift.
 */
@ControllerAdvice
class UnreadBadge {

    private final MessagingService messaging;

    UnreadBadge(MessagingService messaging) {
        this.messaging = messaging;
    }

    @ModelAttribute("unread")
    Integer unread(HttpServletRequest request) {
        return CurrentMember.get(request)
                .flatMap(member -> messaging.unreadFor(member.id()))
                .orElse(null);
    }
}
