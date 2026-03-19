package com.billmate.slack.command;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.service.PaymentRecordService;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.service.SubscriptionService;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.service.UserService;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * /billmate seed [months]  (기본값 6)
 * 테스트용: 활성 구독 전체에 대해 N개월치 결제 이력을 일괄 생성.
 * billmate.features.seed=true 설정 시에만 빈으로 등록됨.
 */
@ConditionalOnProperty(name = "billmate.features.seed", havingValue = "true", matchIfMissing = false)
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedCommandHandler implements CommandHandler {

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

        int months = 6;
        if (parts.length > 1) {
            try {
                months = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                // 기본값 6 유지
            }
        }

        try {
            User user = userService.getOrCreateUser(slackUserId, workspaceId, userName);
            List<Subscription> subs = subscriptionService.listEntitiesByUser(user);

            if (subs.isEmpty()) {
                ctx.respond(r -> r.responseType("ephemeral").text("등록된 구독이 없어요. 구독을 추가한 뒤 다시 시도해주세요."));
                return;
            }

            Map<String, Integer> results = new LinkedHashMap<>();
            int totalCreated = 0;
            final int finalMonths = months;

            for (Subscription sub : subs) {
                Set<LocalDate> existingDates = paymentRecordService.getHistory(sub)
                        .stream().map(PaymentRecord::getBilledAt).collect(Collectors.toSet());
                int created = 0;
                for (int i = finalMonths - 1; i >= 0; i--) {
                    LocalDate targetMonth = LocalDate.now().minusMonths(i);
                    int lastDay = YearMonth.of(targetMonth.getYear(), targetMonth.getMonth()).lengthOfMonth();
                    LocalDate billedAt = targetMonth.withDayOfMonth(Math.min(sub.getBillingDay(), lastDay));
                    if (!billedAt.isAfter(LocalDate.now()) && !existingDates.contains(billedAt)) {
                        paymentRecordService.record(sub, sub.getAmount(), billedAt);
                        created++;
                    }
                }
                results.put(sub.getServiceName(), created);
                totalCreated += created;
            }

            StringBuilder sb = new StringBuilder("✅ 시드 데이터 생성 완료!\n\n");
            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                sb.append(String.format("*%s*: %d건 추가\n", entry.getKey(), entry.getValue()));
            }
            sb.append(String.format("\n총 %d건의 결제 이력이 생성되었습니다.", totalCreated));

            String message = sb.toString();
            ctx.respond(r -> r.responseType("ephemeral").text(message));

        } catch (Exception e) {
            log.error("Seed command error", e);
        }
    }
}
