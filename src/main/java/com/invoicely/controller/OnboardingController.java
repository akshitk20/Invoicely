package com.invoicely.controller;

import com.invoicely.model.User;
import com.invoicely.service.UserService;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OnboardingController {

    private final UserService userService;

    public OnboardingController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/onboarding")
    public String showOnboarding(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("user", user);
        return "onboarding";
    }

    @PostMapping("/onboarding")
    public String saveOnboarding(@AuthenticationPrincipal OAuth2User oAuth2User,
                                  @RequestParam String businessName,
                                  @RequestParam String address,
                                  @RequestParam String state,
                                  @RequestParam(required = false) String gstin,
                                  @RequestParam(required = false) String pan,
                                  @RequestParam(required = false) String defaultSacCode) {
        User user = userService.getCurrentUser(oAuth2User);
        user.setBusinessName(businessName);
        user.setAddress(address);
        user.setState(state);
        user.setGstin(gstin);
        user.setPan(pan);
        user.setDefaultSacCode(defaultSacCode);
        user.setOnboardingComplete(true);
        userService.save(user);
        return "redirect:/dashboard";
    }
}
