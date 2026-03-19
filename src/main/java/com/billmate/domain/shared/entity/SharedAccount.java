package com.billmate.domain.shared.entity;

import com.billmate.common.entity.BaseTimeEntity;
import com.billmate.domain.subscription.entity.Subscription;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "shared_accounts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_shared_sub_member",
        columnNames = {"subscription_id", "member_slack_user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SharedAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(length = 30, nullable = false)
    private String memberSlackUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SharedRole role = SharedRole.MEMBER;

    @Column(precision = 10, scale = 2)
    private BigDecimal splitAmount;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate joinedAt = LocalDate.now();
}
