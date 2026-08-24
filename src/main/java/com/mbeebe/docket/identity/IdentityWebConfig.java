package com.mbeebe.docket.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class IdentityWebConfig implements WebMvcConfigurer {

    private final SessionService sessionService;

    IdentityWebConfig(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) {
                sessionService.resolve(request)
                        .ifPresent(member -> request.setAttribute(CurrentMember.ATTRIBUTE, member));
                return true;
            }
        });
    }

    /** What templates see of the signed-in member — a view model, never the entity. */
    public record CurrentMemberView(String email, String initial) {
    }

    @ControllerAdvice
    static class CurrentMemberModelAdvice {

        @ModelAttribute("currentMember")
        CurrentMemberView currentMember(HttpServletRequest request) {
            return CurrentMember.get(request)
                    .map(member -> new CurrentMemberView(member.email(),
                            member.email().substring(0, 1).toUpperCase(java.util.Locale.ROOT)))
                    .orElse(null);
        }
    }
}
