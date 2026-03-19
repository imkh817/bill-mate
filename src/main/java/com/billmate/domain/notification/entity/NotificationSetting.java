package com.billmate.domain.notification.entity;

import com.billmate.common.entity.BaseTimeEntity;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(nullable = false)
    private int daysBeforeBilling;

    @Column(nullable = false)
    @Builder.Default
    private boolean isEnabled = true;

    public void toggleEnabled() {
        this.isEnabled = !this.isEnabled;
    }
}
