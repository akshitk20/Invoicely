package com.invoicely.repository;

import com.invoicely.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRazorpaySubscriptionId(String subscriptionId);
}
