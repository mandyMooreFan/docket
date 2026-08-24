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

import java.time.YearMonth;
import java.util.Optional;

@Controller
class PositionController {

    private final ProfileService service;
    private final WorkChangeAnnouncer workChanges;

    PositionController(ProfileService service, WorkChangeAnnouncer workChanges) {
        this.service = service;
        this.workChanges = workChanges;
    }

    @PostMapping("/profile/positions")
    String add(@RequestParam String title, @RequestParam String company,
               @RequestParam int startMonth, @RequestParam int startYear,
               @RequestParam String description,
               @RequestParam(required = false) String share,
               HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (title.isBlank()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("edit", service.editView(member.get().id()));
            model.addAttribute("error", "A position needs a title.");
            return "profile-edit";
        }
        service.addPosition(member.get().id(), title, company,
                YearMonth.of(startYear, startMonth), description);
        // §5.2.3: only the box ticked at this moment shares — never automatic.
        if (share != null) {
            workChanges.roleStarted(member.get().id(), title.strip(), company.strip());
        }
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/positions/{id}/end")
    String end(@PathVariable long id, @RequestParam int endMonth, @RequestParam int endYear,
               @RequestParam(required = false) String share, HttpServletRequest request) {
        long memberId = requireMember(request);
        PositionView ended = service.endPosition(memberId, id, YearMonth.of(endYear, endMonth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // §5.2.3: only the box ticked at this moment shares — never automatic.
        if (share != null) {
            workChanges.roleEnded(memberId, ended.title(), ended.company());
        }
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/positions/{id}/delete")
    String delete(@PathVariable long id, HttpServletRequest request) {
        long memberId = requireMember(request);
        if (!service.deletePosition(memberId, id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/profile/edit";
    }

    private long requireMember(HttpServletRequest request) {
        return CurrentMember.get(request).map(Member::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
