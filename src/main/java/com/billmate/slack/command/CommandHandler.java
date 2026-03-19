package com.billmate.slack.command;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;

public interface CommandHandler {
    Response handle(SlashCommandRequest req, SlashCommandContext ctx) throws Exception;
}
