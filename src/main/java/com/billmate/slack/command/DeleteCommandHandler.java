package com.billmate.slack.command;

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
 * /billmate delete [id]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteCommandHandler implements CommandHandler {

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
            if (parts.length < 2) {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("사용법: `/billmate delete [id]`")));
                return;
            }

            Long id = Long.parseLong(parts[1]);
            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            subscriptionService.delete(user, id);

            ctx.client().chatPostMessage(r -> r
                    .channel(slackUserId)
                    .text("구독(ID: " + id + ")이 삭제되었습니다."));

        } catch (NumberFormatException e) {
            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("유효한 구독 ID를 입력하세요.")));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
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
            log.error("Delete command error", e);
        }
    }
}
