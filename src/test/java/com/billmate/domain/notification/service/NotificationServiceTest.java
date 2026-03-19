package com.billmate.domain.notification.service;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.notification.repository.NotificationSettingRepository;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    NotificationSettingRepository notificationSettingRepository;

    @InjectMocks
    NotificationService notificationService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder().slackUserId("U001").slackWorkspaceId("W001").displayName("Alice").build();
        subscription = Subscription.builder()
                .user(user)
                .serviceName("Netflix")
                .category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900"))
                .billingDay(25)
                .billingCycle(BillingCycle.MONTHLY)
                .isActive(true)
                .startedAt(LocalDate.now())
                .build();
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("UC-NS1: createDefaults() → D-3, D-1, D-0 총 3개 NotificationSetting 저장")
    void createDefaults_savesThreeSettings() {
        notificationService.createDefaults(user, subscription);

        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        then(notificationSettingRepository).should(times(3)).save(captor.capture());

        List<Integer> savedDays = captor.getAllValues().stream()
                .map(NotificationSetting::getDaysBeforeBilling)
                .sorted()
                .toList();
        assertThat(savedDays).containsExactly(0, 1, 3);
    }

    @Test
    @DisplayName("UC-NS2: addCustomNotification() — 이미 같은 daysBefore 존재 시 중복 저장 안 함")
    void addCustomNotification_alreadyExists_noSave() {
        NotificationSetting existing = NotificationSetting.builder()
                .user(user).subscription(subscription).daysBeforeBilling(7).isEnabled(true).build();
        given(notificationSettingRepository.findByUserAndSubscription(user, subscription))
                .willReturn(List.of(existing));

        notificationService.addCustomNotification(user, subscription, 7);

        then(notificationSettingRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("UC-NS3: addCustomNotification() — 새로운 daysBefore이면 저장 호출")
    void addCustomNotification_newDays_savesCalled() {
        given(notificationSettingRepository.findByUserAndSubscription(user, subscription))
                .willReturn(List.of());

        notificationService.addCustomNotification(user, subscription, 7);

        then(notificationSettingRepository).should(times(1)).save(any(NotificationSetting.class));
    }
}
