package com.mbeebe.docket;

import com.mbeebe.docket.identity.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    @GetMapping("/")
    String home(HttpServletRequest request) {
        return CurrentMember.get(request).isPresent() ? "home" : "landing";
    }
}
