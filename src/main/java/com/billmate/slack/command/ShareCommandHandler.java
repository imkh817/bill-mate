package com.billmate.slack.command;

import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.shared.service.SharedAccountService;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * /billmate share add [id] [@user] [splitAmount]
 * /billmate share list [id]
 * /billmate share remove [id] [@user]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShareCommandHandler implements CommandHandler {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SharedAccountService sharedAccountService;

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
                        .blocks(SlackMessageBuilder.buildError(
                                "사용법: `/billmate share add|list|remove [id] [@user]`")));
                return;
            }

            String subCommand = parts[1].toLowerCase();
            Long subscriptionId = Long.parseLong(parts[2]);
            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            Subscription subscription = subscriptionService.getActiveSubscription(user, subscriptionId);

            switch (subCommand) {
                case "add" -> {
                    if (parts.length < 4) {
                        ctx.client().chatPostMessage(r -> r
                                .channel(slackUserId)
                                .blocks(SlackMessageBuilder.buildError(
                                        "사용법: `/billmate share add [id] [@user] [금액]`")));
                        return;
                    }
                    String memberUserId = extractUserId(parts[3]);
                    BigDecimal splitAmount = parts.length > 4 ? new BigDecimal(parts[4]) : null;
                    sharedAccountService.addMember(subscription, memberUserId, splitAmount);
                    ctx.client().chatPostMessage(r -> r
                            .channel(slackUserId)
                            .text("<@" + memberUserId + ">가 공유 멤버로 추가되었습니다."));
                }
                case "list" -> {
                    List<SharedAccount> members = sharedAccountService.getMembers(subscription);
                    ctx.client().chatPostMessage(r -> r
                            .channel(slackUserId)
                            .blocks(SlackMessageBuilder.buildSharedMembers(members)));
                }
                case "remove" -> {
                    if (parts.length < 4) {
                        ctx.client().chatPostMessage(r -> r
                                .channel(slackUserId)
                                .blocks(SlackMessageBuilder.buildError(
                                        "사용법: `/billmate share remove [id] [@user]`")));
                        return;
                    }
                    String memberUserId = extractUserId(parts[3]);
                    sharedAccountService.removeMember(subscription, memberUserId);
                    ctx.client().chatPostMessage(r -> r
                            .channel(slackUserId)
                            .text("<@" + memberUserId + ">가 공유 멤버에서 제거되었습니다."));
                }
                default -> ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("알 수 없는 서브커맨드: " + subCommand)));
            }

        } catch (IllegalArgumentException e) {
            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError(e.getMessage())));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        } catch (Exception e) {
            log.error("Share command error", e);
        }
    }

    private String extractUserId(String mention) {
        // <@U12345> or @U12345 or U12345
        return mention.replaceAll("[<>@]", "").split("\\|")[0];
    }
}
