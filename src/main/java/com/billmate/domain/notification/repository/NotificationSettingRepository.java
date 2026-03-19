package com.billmate.domain.notification.repository;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    List<NotificationSetting> findBySubscription(Subscription subscription);

    @Query("""
        SELECT ns FROM NotificationSetting ns
        JOIN FETCH ns.subscription s
        JOIN FETCH ns.user u
        WHERE ns.isEnabled = true
          AND s.isActive = true
          AND s.billingDay = :targetDay
          AND ns.daysBeforeBilling = :daysBeforeBilling
        """)
    List<NotificationSetting> findDueNotifications(
            @Param("targetDay") int targetDay,
            @Param("daysBeforeBilling") int daysBeforeBilling);

    List<NotificationSetting> findByUserAndSubscription(User user, Subscription subscription);
}
