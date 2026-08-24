package com.mbeebe.docket.graph;

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

/**
 * The app bar's Network destination (§13.4's nav surface for the rail): your
 * pending incoming requests and your connection list. Nothing else — no
 * suggestions, no people-you-may-know.
 */
@Controller
class NetworkController {

    private final GraphService service;

    NetworkController(GraphService service) {
        this.service = service;
    }

    @GetMapping("/network")
    String network(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("network", service.networkFor(member.get().id()));
        return "network";
    }

    /** Accepting is keyed by the requester, not a row id: one pending request per pair. */
    @PostMapping("/network/accept/{requesterId}")
    String accept(@PathVariable long requesterId, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (!service.accept(member.get(), requesterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/network";
    }

    @PostMapping("/network/decline/{requesterId}")
    String decline(@PathVariable long requesterId, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (!service.decline(member.get(), requesterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/network";
    }
}
