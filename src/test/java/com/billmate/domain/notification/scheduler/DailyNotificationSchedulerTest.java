package com.billmate.domain.notification.scheduler;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.notification.service.NotificationService;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import com.billmate.slack.install.InstallationTokenResolver;
import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DailyNotificationSchedulerTest {

    @Mock
    NotificationService notificationService;

    @Mock
    App slackApp;

    @Mock
    InstallationTokenResolver tokenResolver;

    @InjectMocks
    DailyNotificationScheduler scheduler;

    private MethodsClient methodsClient;
    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() throws Exception {
        methodsClient = mock(MethodsClient.class);
        given(slackApp.client()).willReturn(methodsClient);
        given(tokenResolver.getBotToken(anyString())).willReturn("xoxb-test");
        given(methodsClient.chatPostMessage(ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()))
                .willReturn(new ChatPostMessageResponse());

        user = User.builder().slackUserId("U001").slackWorkspaceId("T001").displayName("Alice").build();
        subscription = Subscription.builder()
                .user(user).serviceName("Netflix").category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900")).billingDay(25)
                .billingCycle(BillingCycle.MONTHLY).isActive(true)
                .cancelUrl(null).startedAt(LocalDate.now()).build();

        given(notificationService.findDueNotifications(anyInt(), anyInt())).willReturn(List.of());
    }

    @Test
    @DisplayName("UC-DS1: sendDailyNotifications() → 4개 offset(0,1,3,7)에 대해 findDueNotifications 호출")
    void sendDailyNotifications_callsFourOffsets() {
        LocalDate fixedToday = LocalDate.of(2024, 3, 15);
        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedToday);

            scheduler.sendDailyNotifications();
        }

        then(notificationService).should(times(1)).findDueNotifications(15, 0);
        then(notificationService).should(times(1)).findDueNotifications(16, 1);
        then(notificationService).should(times(1)).findDueNotifications(18, 3);
        then(notificationService).should(times(1)).findDueNotifications(22, 7);
    }

    @Test
    @DisplayName("UC-DS2: 2월 기준 offset=3 → targetDay=28로 clamp")
    void sendDailyNotifications_february_clampsToLastDay() {
        LocalDate fixedToday = LocalDate.of(2024, 2, 25);
        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedToday);

            scheduler.sendDailyNotifications();
        }

        then(notificationService).should(times(1)).findDueNotifications(28, 3);
    }

    @Test
    @DisplayName("UC-DS3: 알림 전송 시 tokenResolver.getBotToken(workspaceId)로 토큰 조회")
    void sendDailyNotifications_usesTokenResolverPerWorkspace() throws Exception {
        LocalDate fixedToday = LocalDate.of(2024, 3, 15);

        NotificationSetting ns = NotificationSetting.builder()
                .user(user).subscription(subscription).daysBeforeBilling(0).isEnabled(true).build();
        given(notificationService.findDueNotifications(15, 0)).willReturn(List.of(ns));

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedToday);
            scheduler.sendDailyNotifications();
        }

        then(tokenResolver).should(atLeastOnce()).getBotToken("T001");
    }

    @Test
    @DisplayName("UC-DS4: Slack API 예외 발생 시 다음 알림도 계속 처리됨")
    void sendDailyNotifications_slackException_continuesProcessing() throws Exception {
        LocalDate fixedToday = LocalDate.of(2024, 3, 15);

        NotificationSetting ns1 = NotificationSetting.builder()
                .user(user).subscription(subscription).daysBeforeBilling(0).isEnabled(true).build();
        NotificationSetting ns2 = NotificationSetting.builder()
                .user(user).subscription(subscription).daysBeforeBilling(0).isEnabled(true).build();

        given(notificationService.findDueNotifications(15, 0)).willReturn(List.of(ns1, ns2));
        given(methodsClient.chatPostMessage(ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()))
                .willThrow(new RuntimeException("Slack error"))
                .willReturn(new ChatPostMessageResponse());

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedToday);

            assertThatCode(() -> scheduler.sendDailyNotifications()).doesNotThrowAnyException();
        }

        then(methodsClient).should(times(2)).chatPostMessage(
                ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any());
    }
}
