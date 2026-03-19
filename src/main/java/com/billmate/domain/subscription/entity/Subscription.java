package com.billmate.domain.subscription.entity;

import com.billmate.common.entity.BaseTimeEntity;
import com.billmate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_sub_user_active", columnList = "user_id, is_active"),
        @Index(name = "idx_sub_user_category_active", columnList = "user_id, category, is_active"),
        @Index(name = "idx_sub_billing_day", columnList = "billing_day")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100, nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionCategory category;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "KRW";

    @Column(nullable = false)
    private int billingDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column(length = 500)
    private String cancelUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column
    private LocalDate startedAt;

    @Column
    private LocalDate endedAt;

    @Column(length = 100)
    private String customCategoryName;

    public void deactivate() {
        this.isActive = false;
        this.endedAt = LocalDate.now();
    }
}
