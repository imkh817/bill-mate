package com.billmate.domain.subscription.repository;

import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserAndIsActiveTrue(User user);

    List<Subscription> findByUserAndCategoryAndIsActiveTrue(User user, SubscriptionCategory category);

    Optional<Subscription> findByIdAndUserAndIsActiveTrue(Long id, User user);

    @Query("SELECT s FROM Subscription s WHERE s.isActive = true AND s.billingDay = :billingDay")
    List<Subscription> findActiveByBillingDay(@Param("billingDay") int billingDay);

    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.isActive = true ORDER BY s.createdAt ASC")
    List<Subscription> findActiveByUserOrderByCreatedAt(@Param("user") User user);
}
