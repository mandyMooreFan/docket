package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

/**
 * Home. For a signed-in member, "/" is the feed (§5.1) in the §2.3 rail layout;
 * logged out it stays the landing page. Viewing the feed is what advances the
 * read position — the §5.1 tracked state.
 */
@Controller
class FeedController {

    private final FeedService service;

    FeedController(FeedService service) {
        this.service = service;
    }

    @GetMapping("/")
    String home(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "landing";
        }
        model.addAttribute("feed", service.feedFor(member.get(), true));
        return "feed";
    }
}
