package com.billmate.slack.command;

import com.billmate.domain.report.service.ReportService;
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

import java.time.YearMonth;

/**
 * /billmate report
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportCommandHandler implements CommandHandler {

    private final UserService userService;
    private final ReportService reportService;

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
            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            reportService.sendReportToUser(user, lastMonth);
        } catch (Exception e) {
            log.error("Report command error", e);
            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(slackUserId)
                        .blocks(SlackMessageBuilder.buildError("리포트 생성 중 오류가 발생했습니다.")));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
}
