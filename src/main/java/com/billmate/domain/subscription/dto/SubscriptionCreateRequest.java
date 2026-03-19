package com.billmate.domain.subscription.dto;

import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SubscriptionCreateRequest {
    private String serviceName;
    private SubscriptionCategory category;
    private BigDecimal amount;
    private String currency;
    private int billingDay;
    private BillingCycle billingCycle;
    private String cancelUrl;
    private String customCategoryName;
}
