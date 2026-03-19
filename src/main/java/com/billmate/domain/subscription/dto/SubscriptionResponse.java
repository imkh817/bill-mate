package com.billmate.domain.subscription.dto;

import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.slack.message.SlackMessageBuilder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SubscriptionResponse {
    private Long id;
    private String serviceName;
    private SubscriptionCategory category;
    private BigDecimal amount;
    private String currency;
    private int billingDay;
    private BillingCycle billingCycle;
    private String cancelUrl;
    private LocalDate startedAt;
    private String customCategoryName;

    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .serviceName(subscription.getServiceName())
                .category(subscription.getCategory())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .billingDay(subscription.getBillingDay())
                .billingCycle(subscription.getBillingCycle())
                .cancelUrl(subscription.getCancelUrl())
                .startedAt(subscription.getStartedAt())
                .customCategoryName(subscription.getCustomCategoryName())
                .build();
    }

    public String getCategoryDisplay() {
        return customCategoryName != null
                ? customCategoryName
                : SlackMessageBuilder.categoryEmoji(category) + "  " + SlackMessageBuilder.categoryLabel(category);
    }
}
