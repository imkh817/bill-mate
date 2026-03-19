package com.billmate.slack.command;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.service.PaymentRecordService;
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

import java.util.List;

/**
 * /billmate history [id]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryCommandHandler implements CommandHandler {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final PaymentRecordService paymentRecordService;

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
            if (parts.length < 2) {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("사용법: `/billmate history [id]`")));
                return;
            }

            Long id = Long.parseLong(parts[1]);
            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            Subscription subscription = subscriptionService.getActiveSubscription(user, id);
            List<PaymentRecord> records = paymentRecordService.getHistory(subscription);

            ctx.client().chatPostMessage(r -> r
                    .channel(slackUserId)
                    .blocks(SlackMessageBuilder.buildPaymentHistory(subscription.getServiceName(), records)));

        } catch (IllegalArgumentException e) {
            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError(e.getMessage())));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        } catch (Exception e) {
            log.error("History command error", e);
        }
    }
}
