package com.billmate.support;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.entity.PaymentStatus;
import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.shared.entity.SharedRole;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TestFixtures {

    public static User user(String slackUserId) {
        return User.builder()
                .slackUserId(slackUserId)
                .slackWorkspaceId("W001")
                .displayName("Test User")
                .build();
    }

    public static Subscription subscription(User user, String serviceName,
                                             SubscriptionCategory category,
                                             int billingDay) {
        return Subscription.builder()
                .user(user)
                .serviceName(serviceName)
                .category(category)
                .amount(new BigDecimal("9900"))
                .currency("KRW")
                .billingDay(billingDay)
                .billingCycle(BillingCycle.MONTHLY)
                .isActive(true)
                .startedAt(LocalDate.now())
                .build();
    }

    public static NotificationSetting notificationSetting(User user, Subscription sub,
                                                           int daysBeforeBilling,
                                                           boolean isEnabled) {
        return NotificationSetting.builder()
                .user(user)
                .subscription(sub)
                .daysBeforeBilling(daysBeforeBilling)
                .isEnabled(isEnabled)
                .build();
    }

    public static PaymentRecord paymentRecord(Subscription sub, BigDecimal amount,
                                               LocalDate billedAt) {
        return PaymentRecord.builder()
                .subscription(sub)
                .amount(amount)
                .billedAt(billedAt)
                .status(PaymentStatus.PAID)
                .build();
    }

    public static SharedAccount sharedAccount(Subscription sub, String memberSlackUserId) {
        return SharedAccount.builder()
                .subscription(sub)
                .memberSlackUserId(memberSlackUserId)
                .role(SharedRole.MEMBER)
                .build();
    }
}
