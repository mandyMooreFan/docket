package com.mbeebe.docket;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The living reference for SPEC.md §2 — every token, exercised on one page. */
@Controller
class StyleguideController {

    @GetMapping("/styleguide")
    String styleguide() {
        return "styleguide";
    }
}
