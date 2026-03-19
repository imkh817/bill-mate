package com.billmate.slack.command;

import com.billmate.domain.notification.service.NotificationService;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.service.SubscriptionService;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.service.UserService;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * /billmate notify [id] [days]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyCommandHandler implements CommandHandler {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    @Override
    public Response handle(SlashCommandRequest req, SlashCommandContext ctx) throws Exception {
        ctx.ack();
        sendAsync(req, ctx);
        return ctx.ack();
    }

    @Async
    public void sendAsync(SlashCommandRequest req, SlashCommandContext ctx) {
        String slackUserId = req.getPayload().getUserId();
        String workspaceId = req.getPayload().getTeamId();
        String userName = req.getPayload().getUserName();
        String text = req.getPayload().getText().trim();
        String[] parts = text.split("\\s+");

        try {
            if (parts.length < 3) {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("사용법: `/billmate notify [id] [days]`")));
                return;
            }

            Long id = Long.parseLong(parts[1]);
            int days = Integer.parseInt(parts[2]);

            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            Subscription subscription = subscriptionService.getActiveSubscription(user, id);
            notificationService.addCustomNotification(user, subscription, days);

            ctx.client().chatPostMessage(r -> r
                    .channel(slackUserId)
                    .text(String.format("D-%d 알림이 추가되었습니다.", days)));

        } catch (Exception e) {
            log.error("Notify command error", e);
        }
    }
}
