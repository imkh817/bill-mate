package com.billmate.slack;

import com.billmate.slack.command.*;
import com.slack.api.bolt.App;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackCommandRouter {

    private final App app;
    private final AddCommandHandler addHandler;
    private final ListCommandHandler listHandler;
    private final DeleteCommandHandler deleteHandler;
    private final NotifyCommandHandler notifyHandler;
    private final ReportCommandHandler reportHandler;
    private final ShareCommandHandler shareHandler;
    private final HistoryCommandHandler historyHandler;
    private final HelpCommandHandler helpHandler;
    private final ClearCommandHandler clearHandler;

    // billmate.features.seed=true 일 때만 빈이 등록되므로 optional 주입
    @Autowired(required = false)
    private SeedCommandHandler seedHandler;

    @PostConstruct
    public void registerHandlers() {
        app.command("/billmate", (req, ctx) -> {
            String text = req.getPayload().getText();
            String sub = (text == null || text.isBlank()) ? "help"
                    : text.trim().split("\\s+")[0].toLowerCase();

            log.info("Received /billmate command: sub={}, user={}", sub, req.getPayload().getUserId());

            try {
                return switch (sub) {
                    case "add"     -> addHandler.handle(req, ctx);
                    case "list"    -> listHandler.handle(req, ctx);
                    case "delete"  -> deleteHandler.handle(req, ctx);
                    case "notify"  -> notifyHandler.handle(req, ctx);
                    case "report"  -> reportHandler.handle(req, ctx);
                    case "share"   -> shareHandler.handle(req, ctx);
                    case "history" -> historyHandler.handle(req, ctx);
                    case "clear"   -> clearHandler.handle(req, ctx);
                    case "seed"    -> seedHandler != null
                            ? seedHandler.handle(req, ctx)
                            : helpHandler.handle(req, ctx);
                    default        -> helpHandler.handle(req, ctx);
                };
            } catch (Exception e) {
                log.error("Unhandled error in /billmate command", e);
                return ctx.ack(":x: 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            }
        });
    }
}
