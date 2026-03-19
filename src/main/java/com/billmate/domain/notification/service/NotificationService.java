package com.billmate.domain.notification.service;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.notification.repository.NotificationSettingRepository;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int[] DEFAULT_DAYS_BEFORE = {3, 1, 0};

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void createDefaults(User user, Subscription subscription) {
        for (int daysBefore : DEFAULT_DAYS_BEFORE) {
            notificationSettingRepository.save(
                    NotificationSetting.builder()
                            .user(user)
                            .subscription(subscription)
                            .daysBeforeBilling(daysBefore)
                            .isEnabled(true)
                            .build()
            );
        }
    }

    @Transactional
    public void addCustomNotification(User user, Subscription subscription, int daysBeforeBilling) {
        List<NotificationSetting> existing = notificationSettingRepository
                .findByUserAndSubscription(user, subscription);
        boolean alreadyExists = existing.stream()
                .anyMatch(ns -> ns.getDaysBeforeBilling() == daysBeforeBilling);
        if (!alreadyExists) {
            notificationSettingRepository.save(
                    NotificationSetting.builder()
                            .user(user)
                            .subscription(subscription)
                            .daysBeforeBilling(daysBeforeBilling)
                            .isEnabled(true)
                            .build()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationSetting> getByUserAndSubscription(User user, Subscription subscription) {
        return notificationSettingRepository.findByUserAndSubscription(user, subscription);
    }

    @Transactional
    public void deleteNotification(User user, Long settingId) {
        NotificationSetting setting = notificationSettingRepository.findById(settingId)
                .orElseThrow(() -> new IllegalArgumentException("NotificationSetting not found: " + settingId));
        if (!setting.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized");
        }
        notificationSettingRepository.delete(setting);
    }

    @Transactional(readOnly = true)
    public List<NotificationSetting> findDueNotifications(int targetDay, int daysBeforeBilling) {
        return notificationSettingRepository.findDueNotifications(targetDay, daysBeforeBilling);
    }
}
