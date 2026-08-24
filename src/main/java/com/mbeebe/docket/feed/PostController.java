package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Controller
class PostController {

    private final PostService postService;
    private final FeedService feedService;
    private final CapabilityService capabilities;

    PostController(PostService postService, FeedService feedService,
                   CapabilityService capabilities) {
        this.postService = postService;
        this.feedService = feedService;
        this.capabilities = capabilities;
    }

    /** §3.2: writing a Post is a Capability, earned by Completeness. */
    @PostMapping("/posts")
    String compose(@RequestParam String body,
                   @RequestParam(value = "images", required = false) List<MultipartFile> images,
                   HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        if (capabilities.may(member.get().id(), Capability.POST) != CapabilityAnswer.YES) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Posting opens when your profile is complete.");
        }
        try {
            long postId = postService.compose(member.get(), body, images);
            return "redirect:/posts/" + postId;
        } catch (PostService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("feed", feedService.feedFor(member.get(), false));
            model.addAttribute("error", refused.getMessage());
            return "feed";
        }
    }

    @GetMapping("/posts/{id}")
    String page(@PathVariable long id, HttpServletRequest request, Model model) {
        model.addAttribute("page", postService.pageFor(id, CurrentMember.get(request))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "post";
    }

    @PostMapping("/posts/{id}/replies")
    String reply(@PathVariable long id, @RequestParam String body,
                 HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        try {
            return switch (postService.reply(member.get(), id, body)) {
                case DONE -> "redirect:/posts/" + id;
                case NOT_THERE -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                case REFUSED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Replies are open to the author's connections.");
            };
        } catch (PostService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("page", postService.pageFor(id, member)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
            model.addAttribute("error", refused.getMessage());
            return "post";
        }
    }

    /** §5.3: the author curates their thread — remove a Reply, or close it. */
    @PostMapping("/posts/{postId}/replies/{replyId}/remove")
    String removeReply(@PathVariable long postId, @PathVariable long replyId,
                       HttpServletRequest request) {
        Member member = requireMember(request);
        if (!postService.removeReply(member, postId, replyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/posts/{id}/close")
    String closeThread(@PathVariable long id, HttpServletRequest request) {
        Member member = requireMember(request);
        if (!postService.closeThread(member, id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/posts/" + id;
    }

    private Member requireMember(HttpServletRequest request) {
        return CurrentMember.get(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
