package com.mbeebe.docket.messaging;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.Images;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * The inbox, one Thread, and the Thread's polled refresh (§7.2). A Thread is
 * addressed by the other person, never by a thread id — there is exactly one
 * Thread per pair (ADR-0001), so the pair is the address, and no id has to be
 * looked up or leaked to name a correspondence.
 *
 * <p>A Thread you have no business with does not exist: 404, no placeholder,
 * the same shape the profile and queue pages already use. A Thread you may read
 * but not write to answers 200 and swaps the composer for one honest sentence —
 * the same sentence for a Disconnect and for a Block (§7.3, §4.2).
 */
@Controller
class MessagesController {

    private final MessagingService messaging;

    MessagesController(MessagingService messaging) {
        this.messaging = messaging;
    }

    /** §7.2: the inbox is a list of people. §13.4's empty copy lives in the template. */
    @GetMapping("/messages")
    String inbox(HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("inbox", messaging.inboxFor(member.get()));
        return "messages";
    }

    @GetMapping("/messages/{otherId}")
    String thread(@PathVariable long otherId, HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("page", page(member.get(), otherId));
        return "messages-thread";
    }

    /**
     * §7.2's polite near-realtime: the open Thread re-fetches its message list
     * on one htmx trigger. No socket, no SSE, no realtime infrastructure — and
     * reading here advances only the reader's own mark, so an open Thread keeps
     * its own badge honest without telling the other person anything.
     */
    @GetMapping("/messages/{otherId}/list")
    String list(@PathVariable long otherId, HttpServletRequest request, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("page", page(member.get(), otherId));
        return "messages-thread :: list";
    }

    /**
     * §7.2: text, links and still images, through the shared upload pipeline.
     * A refusal renders the Thread again with the reason, at 422 — the same
     * shape the composer in the feed uses.
     */
    @PostMapping("/messages/{otherId}")
    String send(@PathVariable long otherId, @RequestParam String body,
                @RequestParam(value = "images", required = false) List<MultipartFile> images,
                HttpServletRequest request, HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        Member author = member.get();
        if (author.id() == otherId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        switch (messaging.writability(author.id(), otherId)) {
            case CLOSED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    MessagingService.CLOSED_NOTE);
            case NO_CAPABILITY -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    MessagingService.NO_CAPABILITY_NOTE);
            case OPEN -> { }
        }
        try {
            messaging.send(author, otherId, body, images);
            return "redirect:/messages/" + otherId;
        } catch (MessagingService.Refused refused) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            model.addAttribute("page", page(author, otherId));
            model.addAttribute("error", refused.getMessage());
            return "messages-thread";
        }
    }

    /**
     * A still image on a Message, served only to the two people in the Thread.
     * Correspondence is private by construction (§10.2), so these bytes
     * deliberately do not travel on the shared /images path: this route re-asks
     * the participation question on every request and caches privately. Storage
     * still goes through the one §10.4 store — this is the read path only.
     */
    @GetMapping("/messages/{otherId}/images/{imageId}")
    ResponseEntity<byte[]> image(@PathVariable long otherId, @PathVariable long imageId,
                                 HttpServletRequest request) {
        Images.StoredImage image = CurrentMember.get(request)
                .flatMap(member -> messaging.imageInThreadWith(member, otherId, imageId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(image.data());
    }

    private ThreadPage page(Member viewer, long otherId) {
        return messaging.threadFor(viewer, otherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
