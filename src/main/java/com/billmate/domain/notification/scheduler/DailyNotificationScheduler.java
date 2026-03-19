package com.billmate.domain.notification.scheduler;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.notification.service.NotificationService;
import com.billmate.slack.install.InstallationTokenResolver;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyNotificationScheduler {

    private final NotificationService notificationService;
    private final App slackApp;
    private final InstallationTokenResolver tokenResolver;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyNotifications() {
        LocalDate today = LocalDate.now();
        int[] offsets = {0, 1, 3, 7};

        for (int daysBeforeBilling : offsets) {
            LocalDate targetDate = today.plusDays(daysBeforeBilling);
            int targetDay = clampBillingDay(targetDate);

            List<NotificationSetting> dueNotifications =
                    notificationService.findDueNotifications(targetDay, daysBeforeBilling);

            for (NotificationSetting ns : dueNotifications) {
                sendNotification(ns, daysBeforeBilling);
            }
        }
    }

    private void sendNotification(NotificationSetting ns, int daysBeforeBilling) {
        try {
            var subscription = ns.getSubscription();
            var user = ns.getUser();

            String botToken = tokenResolver.getBotToken(user.getSlackWorkspaceId());
            slackApp.client().chatPostMessage(r -> r
                    .token(botToken)
                    .channel(user.getSlackUserId())
                    .blocks(SlackMessageBuilder.buildNotification(
                            subscription.getServiceName(),
                            daysBeforeBilling,
                            subscription.getAmount(),
                            subscription.getCurrency(),
                            subscription.getCancelUrl())));
        } catch (Exception e) {
            log.error("Failed to send notification for setting id={}", ns.getId(), e);
        }
    }

    /**
     * billingDay=31인 경우 해당 월의 마지막 날로 clamp
     */
    private int clampBillingDay(LocalDate date) {
        int maxDay = YearMonth.of(date.getYear(), date.getMonth()).lengthOfMonth();
        return Math.min(date.getDayOfMonth(), maxDay);
    }
}
