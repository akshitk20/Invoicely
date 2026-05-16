package com.invoicely.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String state;

    @Column(length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    @Column(length = 10)
    private String defaultSacCode;

    @Column(length = 20)
    @Builder.Default
    private String subscriptionTier = "FREE";

    @Column(length = 50)
    private String razorpaySubscriptionId;

    private LocalDateTime subscriptionExpiresAt;

    @Column(length = 20)
    private String subscriptionStatus;

    @Builder.Default
    private Boolean onboardingComplete = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
