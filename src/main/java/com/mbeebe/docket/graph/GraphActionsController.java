package com.mbeebe.docket.graph;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * The graph actions a Profile page offers (§4.2–4.3, §7.3). Outcome mapping is
 * where the silences live: NOT_THERE is the same 404 a nonexistent member gets,
 * and DONE redirects identically whether a request was delivered or swallowed.
 */
@Controller
class GraphActionsController {

    private final GraphService service;

    GraphActionsController(GraphService service) {
        this.service = service;
    }

    @PostMapping("/p/{memberId}/connect")
    String connect(@PathVariable long memberId,
                   @RequestParam(name = "note", defaultValue = "") String note,
                   HttpServletRequest request) {
        Optional<Member> sender = CurrentMember.get(request);
        if (sender.isEmpty()) {
            return "redirect:/login";
        }
        return switch (service.request(sender.get(), memberId, note)) {
            case DONE -> "redirect:/p/" + memberId;
            case NOT_THERE -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            case REFUSED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        };
    }

    @PostMapping("/p/{memberId}/disconnect")
    String disconnect(@PathVariable long memberId, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        service.disconnect(member.get(), memberId);
        return "redirect:/p/" + memberId;
    }

    /** After a Block the pair cannot see each other, so the only place left to land is /network. */
    @PostMapping("/p/{memberId}/block")
    String block(@PathVariable long memberId, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        return switch (service.block(member.get(), memberId)) {
            case DONE -> "redirect:/network";
            case NOT_THERE, REFUSED -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    @PostMapping("/p/{memberId}/recommend")
    String recommend(@PathVariable long memberId,
                     @RequestParam(name = "text", defaultValue = "") String text,
                     HttpServletRequest request) {
        Optional<Member> author = CurrentMember.get(request);
        if (author.isEmpty()) {
            return "redirect:/login";
        }
        return switch (service.recommend(author.get(), memberId, text)) {
            case DONE -> "redirect:/p/" + memberId;
            case NOT_THERE -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            case REFUSED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        };
    }

    /** Approving is keyed by the author: one recommendation per author per subject. */
    @PostMapping("/recommendations/{authorId}/approve")
    String approve(@PathVariable long authorId, HttpServletRequest request) {
        Optional<Member> subject = CurrentMember.get(request);
        if (subject.isEmpty()) {
            return "redirect:/login";
        }
        if (!service.approve(subject.get(), authorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/p/" + subject.get().id();
    }

    @PostMapping("/recommendations/{authorId}/hide")
    String hide(@PathVariable long authorId, HttpServletRequest request) {
        Optional<Member> subject = CurrentMember.get(request);
        if (subject.isEmpty()) {
            return "redirect:/login";
        }
        if (!service.hide(subject.get(), authorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/p/" + subject.get().id();
    }
}
