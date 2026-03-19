package com.billmate.slack.command;

import com.billmate.domain.subscription.dto.SubscriptionCreateRequest;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.service.SubscriptionService;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.service.UserService;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * /billmate add "Netflix" OTT 15900 25 https://help.netflix.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddCommandHandler implements CommandHandler {

    private final UserService userService;
    private final SubscriptionService subscriptionService;

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

        try {
            // text format: add "ServiceName" CATEGORY AMOUNT BILLING_DAY [CANCEL_URL]
            String text = req.getPayload().getText().trim();
            String[] parts = parseAddCommand(text);

            if (parts.length < 5) {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError(
                                "사용법: `/billmate add \"서비스명\" 카테고리 금액 결제일 [해지URL]`")));
                return;
            }

            String serviceName = parts[1].replaceAll("\"", "");
            SubscriptionCategory category = SubscriptionCategory.valueOf(parts[2].toUpperCase());
            BigDecimal amount = new BigDecimal(parts[3]);
            int billingDay = Integer.parseInt(parts[4]);
            String cancelUrl = parts.length > 5 ? parts[5] : null;

            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            SubscriptionResponse response = subscriptionService.create(user,
                    SubscriptionCreateRequest.builder()
                            .serviceName(serviceName)
                            .category(category)
                            .amount(amount)
                            .billingDay(billingDay)
                            .billingCycle(BillingCycle.MONTHLY)
                            .cancelUrl(cancelUrl)
                            .build());

            ctx.client().chatPostMessage(r -> r
                    .channel(slackUserId)
                    .blocks(SlackMessageBuilder.buildSubscriptionAdded(response)));

        } catch (IllegalArgumentException e) {
            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("잘못된 입력: " + e.getMessage())));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        } catch (SlackApiException | IOException e) {
            log.error("Slack API error", e);
        }
    }

    private String[] parseAddCommand(String text) {
        // Handle quoted service names
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\w+)\\s+\"([^\"]+)\"\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\S+))?");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String[] result = new String[matcher.groupCount() + 1];
            result[0] = matcher.group(1);
            result[1] = "\"" + matcher.group(2) + "\"";
            result[2] = matcher.group(3);
            result[3] = matcher.group(4);
            result[4] = matcher.group(5);
            if (matcher.group(6) != null) {
                result[5] = matcher.group(6);
            }
            return result;
        }
        return text.split("\\s+");
    }
}
