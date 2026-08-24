package com.mbeebe.docket.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** 90-day sliding sessions with a visible list and one-tap sign-out-everywhere (§3.3). */
@Service
public class SessionService {

    static final String COOKIE = "docket_session";

    private final MemberSessionRepository sessions;
    private final Clock clock;

    SessionService(MemberSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public void start(Member member, HttpServletRequest request, HttpServletResponse response) {
        String token = Tokens.generate();
        sessions.save(new MemberSession(Tokens.hash(token), member, clock.instant(), client(request)));
        Cookie cookie = new Cookie(COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) MemberSession.LIFETIME.toSeconds());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    @Transactional
    public Optional<Member> resolve(HttpServletRequest request) {
        return token(request)
                .flatMap(token -> sessions.findByTokenHash(Tokens.hash(token)))
                .filter(session -> session.alive(clock.instant()))
                .map(session -> {
                    session.touch(clock.instant());
                    return session.member();
                });
    }

    @Transactional
    public void signOut(HttpServletRequest request, HttpServletResponse response) {
        token(request).ifPresent(token -> sessions.deleteByTokenHash(Tokens.hash(token)));
        clearCookie(request, response);
    }

    @Transactional
    public void signOutEverywhere(Member member, HttpServletRequest request, HttpServletResponse response) {
        endAllSessions(member);
        clearCookie(request, response);
    }

    /**
     * End every session a Member holds, without a request of their own to clear a
     * cookie from. Termination (§10.3, §11.2) happens while the Member is elsewhere,
     * so the door has to be closable from the other side.
     */
    @Transactional
    public void endAllSessions(Member member) {
        sessions.deleteByMember(member);
    }

    @Transactional(readOnly = true)
    public List<SessionView> sessionsFor(Member member, HttpServletRequest request) {
        String currentHash = token(request).map(Tokens::hash).orElse("");
        return sessions.findByMemberOrderByLastUsedAtDesc(member).stream()
                .map(s -> new SessionView(s.id(), s.client(), s.createdAt(), s.lastUsedAt(),
                        s.tokenHash().equals(currentHash)))
                .toList();
    }

    public record SessionView(Long id, String client, java.time.Instant createdAt,
                              java.time.Instant lastUsedAt, boolean current) {
    }

    private Optional<String> token(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String client(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown client";
        }
        return userAgent.length() > 160 ? userAgent.substring(0, 160) : userAgent;
    }
}
