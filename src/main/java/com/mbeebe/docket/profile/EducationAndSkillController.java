package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
class EducationAndSkillController {

    private final ProfileService service;

    EducationAndSkillController(ProfileService service) {
        this.service = service;
    }

    @PostMapping("/profile/education")
    String addEducation(@RequestParam String institution, @RequestParam String course,
                        @RequestParam(required = false) Integer startYear,
                        @RequestParam(required = false) Integer endYear,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (institution.isBlank()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("edit", service.editView(member.get().id()));
            model.addAttribute("error", "An education entry needs an institution.");
            return "profile-edit";
        }
        service.addEducation(member.get().id(), institution, course, startYear, endYear);
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/education/{id}/delete")
    String deleteEducation(@PathVariable long id, HttpServletRequest request) {
        if (!service.deleteEducation(requireMember(request), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/skills")
    String addSkill(@RequestParam String name, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (!name.isBlank()) {
            service.addSkill(member.get().id(), name);
        }
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/skills/{id}/delete")
    String deleteSkill(@PathVariable long id, HttpServletRequest request) {
        if (!service.deleteSkill(requireMember(request), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/profile/edit";
    }

    private long requireMember(HttpServletRequest request) {
        return CurrentMember.get(request).map(Member::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
