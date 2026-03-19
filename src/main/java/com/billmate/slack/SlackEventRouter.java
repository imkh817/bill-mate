package com.billmate.slack;

import com.billmate.domain.payment.service.PaymentRecordService;
import com.billmate.domain.report.service.ReportService;
import com.billmate.domain.subscription.dto.SubscriptionCreateRequest;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.service.SubscriptionService;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.service.UserService;
import com.billmate.slack.install.InstallationTokenResolver;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.App;
import com.slack.api.model.event.MessageEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackEventRouter {

    private final App app;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final PaymentRecordService paymentRecordService;
    private final ReportService reportService;
    private final ConversationStateStore stateStore;
    private final InstallationTokenResolver tokenResolver;

    @PostConstruct
    public void registerHandlers() {
        registerEventHandlers();
        registerActionHandlers();
    }

    // ── DM 메시지 이벤트 ────────────────────────────────────────────────────────

    private void registerEventHandlers() {
        app.event(MessageEvent.class, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();

            if (event.getBotId() != null || event.getSubtype() != null) {
                return ctx.ack();
            }

            String userId = event.getUser();
            String teamId = payload.getTeamId();
            String text = event.getText() != null ? event.getText().trim() : "";

            ConversationState state = stateStore.get(userId);
            if (state != null) {
                processConversationInput(userId, teamId, text, state);
            } else {
                sendMainMenu(userId, teamId, null);
            }
            return ctx.ack();
        });
    }

    // ── 버튼 액션 핸들러 ────────────────────────────────────────────────────────

    private void registerActionHandlers() {

        app.blockAction("menu_list", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                post(botToken, userId, SlackMessageBuilder.buildSubscriptionList(subs));
                sendMainMenu(userId, teamId, null);
            } catch (Exception e) {
                log.error("menu_list error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        app.blockAction("menu_report", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                User user = userService.getOrCreateUser(userId, teamId, null);
                reportService.sendReportToUser(user, YearMonth.now());
                sendMainMenu(userId, teamId, null);
            } catch (Exception e) {
                log.error("menu_report error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        app.blockAction("menu_add", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                post(botToken, userId, SlackMessageBuilder.buildCategoryButtons());
            } catch (Exception e) {
                log.error("menu_add error", e);
            }
            return ctx.ack();
        });

        app.blockAction("menu_history", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                post(botToken, userId, SlackMessageBuilder.buildSubscriptionButtons(subs, "history"));
            } catch (Exception e) {
                log.error("menu_history error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        app.blockAction("menu_delete", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                post(botToken, userId, SlackMessageBuilder.buildSubscriptionButtons(subs, "delete"));
            } catch (Exception e) {
                log.error("menu_delete error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        app.blockAction("menu_notify", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                String guide = "알림 설정은 슬래시 커맨드를 사용해주세요.\n`/billmate notify [구독ID] [일수]`\n예) `/billmate notify 1 3` — 결제 3일 전 알림";
                post(botToken, userId, SlackMessageBuilder.buildError(guide));
                sendMainMenu(userId, teamId, null);
            } catch (Exception e) {
                log.error("menu_notify error", e);
            }
            return ctx.ack();
        });

        app.blockAction("menu_share", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                String guide = "공유 설정은 슬래시 커맨드를 사용해주세요.\n`/billmate share add [구독ID] [@유저] [금액]`";
                post(botToken, userId, SlackMessageBuilder.buildError(guide));
                sendMainMenu(userId, teamId, null);
            } catch (Exception e) {
                log.error("menu_share error", e);
            }
            return ctx.ack();
        });

        // category_{name} 클릭 → 서비스명 입력 요청
        app.blockAction(Pattern.compile("category_(.+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String categoryCode = actionId.substring("category_".length());

            ConversationState state = new ConversationState();
            state.setStep(ConversationStep.AWAITING_SERVICE_NAME);
            state.setCategoryCode(categoryCode);
            state.setTeamId(teamId);
            stateStore.put(userId, state);

            try {
                String botToken = tokenResolver.getBotToken(teamId);
                String catLabel = SlackMessageBuilder.categoryEmoji(SubscriptionCategory.valueOf(categoryCode))
                        + "  " + SlackMessageBuilder.categoryLabel(SubscriptionCategory.valueOf(categoryCode));
                postText(botToken, userId, String.format("*%s* 를 선택하셨어요.\n\n서비스 이름을 입력해주세요.\n_예) Netflix, Spotify, GitHub_", catLabel));
            } catch (Exception e) {
                log.error("category action error", e);
            }
            return ctx.ack();
        });

        // new_category → 카테고리명 직접 입력
        app.blockAction("new_category", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();

            ConversationState state = new ConversationState();
            state.setStep(ConversationStep.AWAITING_CUSTOM_CATEGORY);
            state.setTeamId(teamId);
            stateStore.put(userId, state);

            try {
                String botToken = tokenResolver.getBotToken(teamId);
                postText(botToken, userId, "카테고리 이름을 직접 입력해주세요.\n_예) 클라우드, 업무 도구_");
            } catch (Exception e) {
                log.error("new_category action error", e);
            }
            return ctx.ack();
        });

        // history_sub_{id} → 결제 이력 표시 후 메인 메뉴
        app.blockAction(Pattern.compile("history_sub_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            long subscriptionId = Long.parseLong(actionId.substring("history_sub_".length()));

            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                var subscription = subscriptionService.getActiveSubscription(user, subscriptionId);
                var records = paymentRecordService.getHistory(subscription);
                post(botToken, userId, SlackMessageBuilder.buildPaymentHistory(records));
                sendMainMenu(userId, teamId, null);
            } catch (Exception e) {
                log.error("history_sub action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // delete_sub_{id} → 삭제 후 메인 메뉴
        app.blockAction(Pattern.compile("delete_sub_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            long subscriptionId = Long.parseLong(actionId.substring("delete_sub_".length()));

            try {
                User user = userService.getOrCreateUser(userId, teamId, null);
                var subscription = subscriptionService.getActiveSubscription(user, subscriptionId);
                String serviceName = subscription.getServiceName();
                subscriptionService.delete(user, subscriptionId);
                sendMainMenu(userId, teamId,
                        String.format("✅  *%s* 구독을 삭제했어요.", serviceName));
            } catch (Exception e) {
                log.error("delete_sub action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });
    }

    // ── 대화 입력 처리 ──────────────────────────────────────────────────────────

    private void processConversationInput(String userId, String teamId, String text, ConversationState state) {
        String resolvedTeamId = state.getTeamId() != null ? state.getTeamId() : teamId;
        try {
            String botToken = tokenResolver.getBotToken(resolvedTeamId);
            switch (state.getStep()) {
                case AWAITING_CUSTOM_CATEGORY -> {
                    state.setCustomCategoryName(text);
                    state.setStep(ConversationStep.AWAITING_SERVICE_NAME);
                    state.setTeamId(resolvedTeamId);
                    stateStore.put(userId, state);
                    postText(botToken, userId, String.format("*%s* 카테고리로 등록할게요.\n\n서비스 이름을 입력해주세요.\n_예) Netflix, Spotify_", text));
                }
                case AWAITING_SERVICE_NAME -> {
                    state.setServiceName(text);
                    state.setStep(ConversationStep.AWAITING_AMOUNT);
                    stateStore.put(userId, state);
                    postText(botToken, userId, String.format("*%s* 를 등록할게요.\n\n월 결제 금액을 입력해주세요.\n_예) 15900_", text));
                }
                case AWAITING_AMOUNT -> {
                    BigDecimal amount = new BigDecimal(text.replaceAll("[^0-9.]", ""));
                    state.setAmount(amount);
                    state.setStep(ConversationStep.AWAITING_BILLING_DAY);
                    stateStore.put(userId, state);
                    postText(botToken, userId, String.format("월 *₩%,.0f* 으로 설정할게요.\n\n매월 몇 일에 결제되나요?\n_예) 25_", amount));
                }
                case AWAITING_BILLING_DAY -> {
                    int billingDay = Integer.parseInt(text.trim());
                    stateStore.remove(userId);

                    User user = userService.getOrCreateUser(userId, resolvedTeamId, null);
                    SubscriptionCategory category = state.getCategoryCode() != null
                            ? SubscriptionCategory.valueOf(state.getCategoryCode())
                            : SubscriptionCategory.OTHER;

                    SubscriptionResponse sub = subscriptionService.create(user,
                            SubscriptionCreateRequest.builder()
                                    .serviceName(state.getServiceName())
                                    .category(category)
                                    .customCategoryName(state.getCustomCategoryName())
                                    .amount(state.getAmount())
                                    .billingDay(billingDay)
                                    .billingCycle(BillingCycle.MONTHLY)
                                    .build());

                    sendMainMenu(userId, resolvedTeamId,
                            String.format("✅  *%s* 구독을 등록했어요!\n매월 *%d일* 결제 · *₩%,.0f*",
                                    sub.getServiceName(), billingDay, state.getAmount()));
                }
            }
        } catch (NumberFormatException e) {
            sendError(userId, resolvedTeamId, "숫자만 입력해주세요.");
        } catch (Exception e) {
            log.error("processConversationInput error for userId={}", userId, e);
            stateStore.remove(userId);
            sendError(userId, resolvedTeamId, "처리 중 오류가 발생했어요. 처음부터 다시 시도해주세요.");
        }
    }

    // ── 유틸 ────────────────────────────────────────────────────────────────────

    private void sendMainMenu(String userId, String teamId, String contextMessage) {
        try {
            String botToken = tokenResolver.getBotToken(teamId);
            post(botToken, userId, SlackMessageBuilder.buildMainMenu(contextMessage));
        } catch (Exception e) {
            log.error("sendMainMenu error", e);
        }
    }

    private void sendError(String userId, String teamId, String message) {
        try {
            String botToken = tokenResolver.getBotToken(teamId);
            post(botToken, userId, SlackMessageBuilder.buildError(message));
        } catch (Exception e) {
            log.error("sendError failed", e);
        }
    }

    private void post(String botToken, String channel, java.util.List<com.slack.api.model.block.LayoutBlock> blocks) throws Exception {
        app.client().chatPostMessage(r -> r.token(botToken).channel(channel).blocks(blocks));
    }

    private void postText(String botToken, String channel, String text) throws Exception {
        app.client().chatPostMessage(r -> r.token(botToken).channel(channel).text(text));
    }
}
