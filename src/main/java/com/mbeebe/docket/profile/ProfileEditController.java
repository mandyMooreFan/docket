package com.mbeebe.docket.profile;

import com.mbeebe.docket.identity.CurrentMember;
import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.Images;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@Controller
class ProfileEditController {

    private final ProfileService service;
    private final Images images;

    ProfileEditController(ProfileService service, Images images) {
        this.service = service;
        this.images = images;
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

    @PostMapping("/profile/dial")
    String dial(@RequestParam Profile.Dial dial, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        service.setDial(member.get().id(), dial);
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/open-to-work")
    String openToWork(@RequestParam Profile.OpenToWork audience, HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        service.setOpenToWork(member.get().id(), audience);
        return "redirect:/profile/edit";
    }

    /**
     * The §4.1 photo, through the one image store (§10.4) — the same door the
     * Company logo and a Post's images go through, so the two hash checks run here
     * without this controller knowing they exist. There is deliberately no second
     * pipeline and no second serving route: the stored id lands on the Profile, and
     * /images/{id} answers for it (see {@link ProfilePhotoAudience}).
     *
     * <p>Editing your own Profile is never gated (§3.2), so this asks only that you
     * are signed in. A photo is not on the §3.2 bar and never becomes one.
     */
    @PostMapping("/profile/photo")
    String photo(@RequestParam("photo") MultipartFile photo, HttpServletRequest request,
                 HttpServletResponse response, Model model) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Images.Stored stored = images.store(bytes, photo.getContentType());
        return switch (stored.outcome()) {
            case STORED -> {
                service.setPhoto(member.get().id(), stored.imageId());
                yield "redirect:/profile/edit";
            }
            case WRONG_TYPE -> reshow(member.get(), response, model,
                    "A photo is a PNG or a JPEG.");
            case TOO_LARGE -> reshow(member.get(), response, model,
                    "Photos are capped at 512 KB.");
            case REFUSED -> reshow(member.get(), response, model,
                    "That image was refused by the upload checks.");
        };
    }

    /**
     * Removal is one button and no confirmation: a Profile with no photo is a
     * perfectly ordinary Profile (§3.2), so going back to initials is an ordinary
     * edit, not a warning-shaped one. The stored image row survives and is simply
     * no longer claimed by anybody, which — by {@code ImageAudiences}' fail-closed
     * default — means it stops being served to everybody.
     */
    @PostMapping("/profile/photo/delete")
    String removePhoto(HttpServletRequest request) {
        Optional<Member> member = CurrentMember.get(request);
        if (member.isEmpty()) {
            return "redirect:/login";
        }
        service.setPhoto(member.get().id(), null);
        return "redirect:/profile/edit";
    }

    private String reshow(Member member, HttpServletResponse response, Model model,
                          String error) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        model.addAttribute("edit", service.editView(member.id()));
        model.addAttribute("error", error);
        return "profile-edit";
    }
}
