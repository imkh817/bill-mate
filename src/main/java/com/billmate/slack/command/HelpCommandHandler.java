package com.billmate.slack.command;

import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * /billmate help
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelpCommandHandler implements CommandHandler {

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
            ctx.client().chatPostMessage(r -> r
                    .channel(slackUserId)
                    .blocks(SlackMessageBuilder.buildHelp()));
        } catch (Exception e) {
            log.error("Help command error", e);
        }
    }
}
