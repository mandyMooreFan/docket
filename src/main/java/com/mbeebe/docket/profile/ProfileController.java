package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
class ProfileController {

    private final ProfileService service;

    ProfileController(ProfileService service) {
        this.service = service;
    }

    /** The stable "me" address: your Profile lives at its public URL, owner or not. */
    @GetMapping("/profile")
    String me(HttpServletRequest request) {
        return CurrentMember.get(request)
                .map(member -> "redirect:/p/" + member.id())
                .orElse("redirect:/login");
    }

    @GetMapping("/p/{memberId}")
    String page(@PathVariable long memberId, HttpServletRequest request, Model model) {
        ProfilePage page = service.pageFor(memberId, CurrentMember.get(request))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("profile", page);
        return "profile";
    }
}
