package com.mbeebe.docket.search;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * The one box (§8): a GET, its query and its one tickable filter in the URL, so
 * a search is a link somebody can send. Browsable signed out for jobs,
 * companies and Posts (§8.4); the People group is account-gated and says so.
 *
 * <p>Over the §10.3 limit the answer is 429 and an honest page — a system
 * control, stated as one, with no suggestion that anything is wrong with the
 * person asking.
 */
@Controller
class SearchController {

    private final SearchService search;
    private final SearchRateLimit limits;

    SearchController(SearchService search, SearchRateLimit limits) {
        this.search = search;
        this.limits = limits;
    }

    @GetMapping("/search")
    String results(@RequestParam(defaultValue = "") String q,
                   @RequestParam(defaultValue = "") String connected,
                   HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> viewer = CurrentMember.get(request);
        // A query with nothing to ask costs no budget: it runs no search.
        if (SearchTerms.prefixQuery(q).isPresent()
                && !limits.accept(viewer, request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return "search-limited";
        }
        model.addAttribute("results", search.search(q, !connected.isBlank(), viewer));
        return "search";
    }
}
