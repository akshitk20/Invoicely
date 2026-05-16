package com.invoicely.service;

import com.invoicely.model.User;
import com.invoicely.repository.InvoiceRepository;
import com.invoicely.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final int FREE_TIER_MONTHLY_LIMIT = 3;

    private final RazorpayClient razorpayClient;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.plan-id}")
    private String planId;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public SubscriptionService(RazorpayClient razorpayClient,
                               UserRepository userRepository,
                               InvoiceRepository invoiceRepository) {
        this.razorpayClient = razorpayClient;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public String createSubscription(User user) throws RazorpayException {
        JSONObject subscriptionRequest = new JSONObject();
        subscriptionRequest.put("plan_id", planId);
        subscriptionRequest.put("total_count", 120);
        subscriptionRequest.put("customer_notify", 1);

        JSONObject notes = new JSONObject();
        notes.put("user_id", String.valueOf(user.getId()));
        notes.put("email", user.getEmail());
        subscriptionRequest.put("notes", notes);

        Subscription subscription = razorpayClient.subscriptions.create(subscriptionRequest);
        String subscriptionId = subscription.get("id");

        user.setRazorpaySubscriptionId(subscriptionId);
        user.setSubscriptionStatus("created");
        userRepository.save(user);

        return subscriptionId;
    }

    @Transactional
    public void activateSubscription(User user) {
        user.setSubscriptionTier("PRO");
        user.setSubscriptionStatus("active");
        user.setSubscriptionExpiresAt(LocalDateTime.now().plusMonths(1));
        userRepository.save(user);
    }

    @Transactional
    public void cancelSubscription(User user) throws RazorpayException {
        if (user.getRazorpaySubscriptionId() != null) {
            JSONObject cancelRequest = new JSONObject();
            cancelRequest.put("cancel_at_cycle_end", 1);
            razorpayClient.subscriptions.cancel(user.getRazorpaySubscriptionId(), cancelRequest);
        }
        user.setSubscriptionStatus("cancelled");
        userRepository.save(user);
    }

    public boolean verifyPaymentSignature(String subscriptionId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_subscription_id", subscriptionId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            return true;
        } catch (RazorpayException e) {
            log.error("Payment signature verification failed", e);
            return false;
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            return true;
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    @Transactional
    public void handleWebhookEvent(String eventType, String payload) {
        JSONObject json = new JSONObject(payload);
        JSONObject subscriptionEntity = json.getJSONObject("payload")
                .getJSONObject("subscription")
                .getJSONObject("entity");

        String subscriptionId = subscriptionEntity.getString("id");
        User user = userRepository.findByRazorpaySubscriptionId(subscriptionId).orElse(null);

        if (user == null) {
            JSONObject notes = subscriptionEntity.optJSONObject("notes");
            if (notes != null && notes.has("user_id")) {
                Long userId = Long.parseLong(notes.getString("user_id"));
                user = userRepository.findById(userId).orElse(null);
            }
        }

        if (user == null) {
            log.warn("No user found for subscription: {}", subscriptionId);
            return;
        }

        switch (eventType) {
            case "subscription.activated":
            case "subscription.charged":
                user.setSubscriptionTier("PRO");
                user.setSubscriptionStatus("active");
                user.setRazorpaySubscriptionId(subscriptionId);
                if (subscriptionEntity.has("current_end")) {
                    long endEpoch = subscriptionEntity.getLong("current_end");
                    user.setSubscriptionExpiresAt(
                            LocalDateTime.ofInstant(Instant.ofEpochSecond(endEpoch), ZoneId.systemDefault()));
                }
                break;
            case "subscription.cancelled":
                user.setSubscriptionStatus("cancelled");
                break;
            case "subscription.halted":
            case "subscription.completed":
            case "subscription.expired":
                user.setSubscriptionTier("FREE");
                user.setSubscriptionStatus("expired");
                user.setRazorpaySubscriptionId(null);
                user.setSubscriptionExpiresAt(null);
                break;
            default:
                log.info("Unhandled webhook event: {}", eventType);
                return;
        }

        userRepository.save(user);
    }

    public boolean canCreateInvoice(User user) {
        if ("PRO".equals(user.getSubscriptionTier())) return true;
        return getMonthlyInvoiceCount(user) < FREE_TIER_MONTHLY_LIMIT;
    }

    public boolean canAccessReports(User user) {
        return "PRO".equals(user.getSubscriptionTier());
    }

    public long getMonthlyInvoiceCount(User user) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
        return invoiceRepository.countByUserIdAndMonth(user.getId(), monthStart, monthEnd);
    }
}
