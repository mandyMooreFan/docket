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
        Optional<Member> viewer = CurrentMember.get(request);
        PostView post = postService.pageFor(id, viewer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("post", post);
        return "post";
    }
}
