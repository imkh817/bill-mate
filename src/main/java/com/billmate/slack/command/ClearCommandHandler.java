package com.billmate.slack.command;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * /billmate clear — DM 대화 기록 정리 (봇이 보낸 메시지 삭제)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClearCommandHandler implements CommandHandler {

    @Override
    public Response handle(SlashCommandRequest req, SlashCommandContext ctx) throws Exception {
        ctx.ack();
        sendAsync(req, ctx);
        return ctx.ack();
    }

    @Async
    public void sendAsync(SlashCommandRequest req, SlashCommandContext ctx) {
        String slackUserId = req.getPayload().getUserId();
        try {
            var openRes = ctx.client().conversationsOpen(r -> r.users(List.of(slackUserId)));
            String channelId = openRes.getChannel().getId();

            String cursor = null;
            int deleted = 0;
            do {
                final String c = cursor;
                var history = ctx.client().conversationsHistory(r -> {
                    var b = r.channel(channelId).limit(200);
                    return c != null ? b.cursor(c) : b;
                });

                for (var msg : history.getMessages()) {
                    if (msg.getBotId() != null) {
                        try {
                            ctx.client().chatDelete(r -> r.channel(channelId).ts(msg.getTs()));
                            deleted++;
                        } catch (Exception ex) {
                            log.warn("메시지 삭제 실패 ts={}: {}", msg.getTs(), ex.getMessage());
                        }
                    }
                }

                cursor = history.isHasMore()
                        ? history.getResponseMetadata().getNextCursor()
                        : null;
            } while (cursor != null);

            log.info("Clear command: {}개 메시지 삭제 (userId={})", deleted, slackUserId);

            // ctx.client().chatPostMessage(r -> r
            //         .channel(slackUserId)
            //         .text("대화 기록이 정리되었습니다. 새롭게 시작해보세요! :sparkles:"));

        } catch (Exception e) {
            log.error("Clear command error (userId={})", slackUserId, e);
        }
    }
}
