package com.mbeebe.docket.skeleton;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    /** The fully-loaded view model handed to the template (SPEC.md §14.2). */
    record HomeView(String note) {
    }

    private final WalkingSkeletonRepository walkingSkeletons;

    HomeController(WalkingSkeletonRepository walkingSkeletons) {
        this.walkingSkeletons = walkingSkeletons;
    }

    @GetMapping("/")
    String home(Model model) {
        String note = walkingSkeletons.findById(1L)
                .map(WalkingSkeleton::note)
                .orElse("The walking skeleton is missing its row.");
        model.addAttribute("view", new HomeView(note));
        return "home";
    }
}
