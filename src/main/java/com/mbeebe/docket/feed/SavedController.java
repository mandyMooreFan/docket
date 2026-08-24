package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/** The private Save (§5.3): keep, let go, and the member's own /saved page. */
@Controller
class SavedController {

    private final PostService service;

    SavedController(PostService service) {
        this.service = service;
    }

    @GetMapping("/saved")
    String saved(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("saved", service.savedFor(member.get()));
        return "saved";
    }

    @PostMapping("/posts/{id}/save")
    String save(@PathVariable long id, HttpServletRequest request) {
        Member member = requireMember(request);
        if (!service.save(member, id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/unsave")
    String unsave(@PathVariable long id, HttpServletRequest request) {
        Member member = requireMember(request);
        service.unsave(member, id);
        return "redirect:/posts/" + id;
    }

    private Member requireMember(HttpServletRequest request) {
        return CurrentMember.get(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
