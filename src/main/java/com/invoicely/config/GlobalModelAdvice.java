package com.invoicely.config;

import com.invoicely.model.User;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("subscriptionTier")
    public String subscriptionTier(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) return "FREE";
        try {
            User user = userService.getCurrentUser(oAuth2User);
            return user.getSubscriptionTier();
        } catch (Exception e) {
            return "FREE";
        }
    }
}
