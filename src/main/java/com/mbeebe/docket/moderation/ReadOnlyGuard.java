package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

/**
 * §10.3's third rung, enforced: a suspended Member is reduced to reading only, and may
 * still sign in.
 *
 * <p>Most of the rung is arithmetic elsewhere — {@link MemberStanding} reports every
 * write Capability as withdrawn, so posting, replying, messaging and connecting all
 * refuse themselves at the gates that already existed. But §3.2 deliberately leaves
 * some writing ungated, because it is not a Capability at all: editing your own
 * Profile, moving the Dial, saving a Post, blocking someone. "Read-only" has to mean
 * those too, and there is no gate on them to compose into.
 *
 * <p>So one interceptor, owned by moderation rather than bolted onto identity's, which
 * refuses state-changing requests outright. It runs after identity's has resolved the
 * viewer, and it is deliberately blunt: every unsafe method is refused, and the two
 * exceptions are listed here rather than scattered.
 *
 * <p>The exceptions matter. A suspended Member must still be able to <em>appeal</em> —
 * §10.3 gives one Appeal, and a suspension that silently swallowed it would make the
 * remedy imaginary. And they must still be able to sign out, because an account you
 * cannot leave is a worse thing than a read-only one.
 */
@Configuration
class ReadOnlyGuard implements WebMvcConfigurer {

    private static final List<String> STILL_ALLOWED = List.of(
            "/appeals",
            "/settings/sessions/sign-out",
            "/settings/sessions/sign-out-everywhere");

    private final MemberStanding standing;

    ReadOnlyGuard(MemberStanding standing) {
        this.standing = standing;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // After identity's, which is what puts the Member on the request at all.
        registry.addInterceptor(new Guard()).order(1);
    }

    private class Guard implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) {
            if (safe(request.getMethod()) || allowed(request.getRequestURI())) {
                return true;
            }
            Optional<Member> member = CurrentMember.get(request);
            if (member.isEmpty()) {
                return true;
            }
            long memberId = member.get().id();
            if (standing.terminated(memberId)) {
                return refuse(response, "This account has been terminated.");
            }
            if (standing.suspended(memberId)) {
                return refuse(response, "Your account is read-only while a moderation "
                        + "decision stands. You can still read everything, and you can appeal.");
            }
            return true;
        }

        private boolean safe(String method) {
            return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
        }

        private boolean allowed(String path) {
            return STILL_ALLOWED.stream().anyMatch(path::startsWith);
        }

        private boolean refuse(HttpServletResponse response, String because) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            // The honest sentence, in the shape the rest of the product refuses things.
            try {
                response.sendError(HttpStatus.FORBIDDEN.value(), because);
            } catch (java.io.IOException ignored) {
                // The response is already committed; the status stands either way.
            }
            return false;
        }
    }
}
