package com.invoicely.controller;

import com.invoicely.model.User;
import com.invoicely.service.SubscriptionService;
import com.invoicely.service.UserService;
import com.razorpay.RazorpayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    public SubscriptionController(SubscriptionService subscriptionService, UserService userService) {
        this.subscriptionService = subscriptionService;
        this.userService = userService;
    }

    @GetMapping("/pricing")
    public String showPricing(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("user", user);
        model.addAttribute("razorpayKeyId", subscriptionService.getRazorpayKeyId());
        model.addAttribute("monthlyInvoiceCount", subscriptionService.getMonthlyInvoiceCount(user));
        return "subscription/pricing";
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createSubscription(@AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = userService.getCurrentUser(oAuth2User);
        try {
            String subscriptionId = subscriptionService.createSubscription(user);
            return ResponseEntity.ok(Map.of(
                    "subscriptionId", subscriptionId,
                    "keyId", subscriptionService.getRazorpayKeyId()
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public String verifyPayment(@AuthenticationPrincipal OAuth2User oAuth2User,
                                @RequestParam String razorpay_subscription_id,
                                @RequestParam String razorpay_payment_id,
                                @RequestParam String razorpay_signature,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);

        boolean valid = subscriptionService.verifyPaymentSignature(
                razorpay_subscription_id, razorpay_payment_id, razorpay_signature);

        if (valid) {
            subscriptionService.activateSubscription(user);
            redirectAttributes.addFlashAttribute("success", "Welcome to Pro! You now have unlimited access.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Payment verification failed. Please contact support.");
        }

        return "redirect:/subscription/manage";
    }

    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                 @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!subscriptionService.verifyWebhookSignature(payload, signature)) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            org.json.JSONObject json = new org.json.JSONObject(payload);
            String eventType = json.getString("event");
            subscriptionService.handleWebhookEvent(eventType, payload);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/manage")
    public String managePlan(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("user", user);
        model.addAttribute("monthlyInvoiceCount", subscriptionService.getMonthlyInvoiceCount(user));
        return "subscription/manage";
    }

    @PostMapping("/cancel")
    public String cancelSubscription(@AuthenticationPrincipal OAuth2User oAuth2User,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        try {
            subscriptionService.cancelSubscription(user);
            redirectAttributes.addFlashAttribute("success", "Subscription cancelled. Pro access continues until the end of your billing period.");
        } catch (RazorpayException e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling subscription: " + e.getMessage());
        }
        return "redirect:/subscription/manage";
    }
}
