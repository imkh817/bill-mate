package com.billmate.domain.subscription.service;

import com.billmate.domain.notification.service.NotificationService;
import com.billmate.domain.subscription.dto.SubscriptionCreateRequest;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.repository.SubscriptionRepository;
import com.billmate.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @Transactional
    public SubscriptionResponse create(User user, SubscriptionCreateRequest request) {
        boolean hasCustomCategory = request.getCustomCategoryName() != null && !request.getCustomCategoryName().isBlank();
        SubscriptionCategory resolvedCategory = hasCustomCategory
                ? SubscriptionCategory.OTHER
                : request.getCategory();

        Subscription subscription = Subscription.builder()
                .user(user)
                .serviceName(request.getServiceName())
                .category(resolvedCategory)
                .customCategoryName(hasCustomCategory ? request.getCustomCategoryName() : null)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "KRW")
                .billingDay(request.getBillingDay())
                .billingCycle(request.getBillingCycle() != null ? request.getBillingCycle() : com.billmate.domain.subscription.entity.BillingCycle.MONTHLY)
                .cancelUrl(request.getCancelUrl())
                .isActive(true)
                .startedAt(LocalDate.now())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        notificationService.createDefaults(user, saved);
        return SubscriptionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listByUser(User user) {
        return subscriptionRepository.findByUserAndIsActiveTrue(user)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listByUserAndCategory(User user, SubscriptionCategory category) {
        return subscriptionRepository.findByUserAndCategoryAndIsActiveTrue(user, category)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @Transactional
    public void delete(User user, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserAndIsActiveTrue(subscriptionId, user)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        subscription.deactivate();
    }

    @Transactional(readOnly = true)
    public Subscription getActiveSubscription(User user, Long subscriptionId) {
        return subscriptionRepository.findByIdAndUserAndIsActiveTrue(subscriptionId, user)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
    }
}
