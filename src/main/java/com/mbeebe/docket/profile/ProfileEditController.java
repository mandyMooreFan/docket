package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
class ProfileEditController {

    private final ProfileService service;

    ProfileEditController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/profile/edit")
    String edit(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("edit", service.editView(member.get().id()));
        return "profile-edit";
    }

    @PostMapping("/profile/basics")
    String basics(@RequestParam String name, @RequestParam String headline,
                  @RequestParam String location, @RequestParam String summary,
                  HttpServletRequest request, Model model, HttpServletResponse response) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (name.isBlank()) {
            // The one validation the name has (§3.3): non-empty. Nothing is saved.
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("edit", service.editView(member.get().id()));
            model.addAttribute("error", "A profile needs a name — it's the only required field.");
            return "profile-edit";
        }
        service.editBasics(member.get().id(), name, headline, location, summary);
        return "redirect:/profile/edit";
    }
}
