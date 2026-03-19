package com.billmate.slack.command;

import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
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
 * /billmate list [CATEGORY]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListCommandHandler implements CommandHandler {

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
        String text = req.getPayload().getText().trim();
        String[] parts = text.split("\\s+");

        try {
            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            List<SubscriptionResponse> subs;

            if (parts.length > 1) {
                try {
                    SubscriptionCategory category = SubscriptionCategory.valueOf(parts[1].toUpperCase());
                    subs = subscriptionService.listByUserAndCategory(user, category);
                } catch (IllegalArgumentException e) {
                    ctx.client().chatPostMessage(r -> r
                            .channel(slackUserId)
                            .blocks(SlackMessageBuilder.buildError("알 수 없는 카테고리: " + parts[1])));
                    return;
                }
            } else {
                subs = subscriptionService.listByUser(user);
            }

            ctx.client().chatPostMessage(r -> r
                    .channel(slackUserId)
                    .blocks(SlackMessageBuilder.buildSubscriptionList(subs)));

        } catch (Exception e) {
            log.error("List command error", e);
        }
    }
}
