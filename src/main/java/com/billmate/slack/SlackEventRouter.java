package com.billmate.slack;

import com.billmate.domain.notification.service.NotificationService;
import com.billmate.domain.payment.service.PaymentRecordService;
import com.billmate.domain.report.service.ReportService;
import com.billmate.domain.subscription.dto.SubscriptionCreateRequest;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.service.SubscriptionService;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.service.UserService;
import com.billmate.slack.install.InstallationTokenResolver;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.App;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.MessageEvent;
import jakarta.annotation.PostConstruct;

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
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
    private final NotificationService notificationService;
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
                sendSubscriptionListOrWelcome(userId, teamId);
            }
            return ctx.ack();
        });
    }

    // ── 버튼 액션 핸들러 ────────────────────────────────────────────────────────

    private void registerActionHandlers() {

        // sub_menu: 구독 항목 드롭다운 (결제이력 / 알림설정 / 삭제)
        app.blockAction("sub_menu", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String value = req.getPayload().getActions().get(0).getSelectedOption().getValue();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                if (value.startsWith("hist_")) {
                    long subId = Long.parseLong(value.substring("hist_".length()));
                    Subscription sub = subscriptionService.getActiveSubscription(user, subId);
                    var records = paymentRecordService.getHistory(sub);
                    update(botToken, channelId, messageTs,
                            SlackMessageBuilder.buildPaymentHistory(sub.getServiceName(), records));
                } else if (value.startsWith("ntf_")) {
                    long subId = Long.parseLong(value.substring("ntf_".length()));
                    Subscription sub = subscriptionService.getActiveSubscription(user, subId);
                    var currentSettings = notificationService.getByUserAndSubscription(user, sub);
                    update(botToken, channelId, messageTs,
                            SlackMessageBuilder.buildNotifyOptions(SubscriptionResponse.from(sub), currentSettings));
                } else if (value.startsWith("del_")) {
                    long subId = Long.parseLong(value.substring("del_".length()));
                    Subscription sub = subscriptionService.getActiveSubscription(user, subId);
                    update(botToken, channelId, messageTs,
                            SlackMessageBuilder.buildDeleteConfirm(SubscriptionResponse.from(sub)));
                }
            } catch (Exception e) {
                log.error("sub_menu error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // 목록 보기
        app.blockAction("menu_list", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                update(botToken, channelId, messageTs, SlackMessageBuilder.buildSubscriptionList(subs));
            } catch (Exception e) {
                log.error("menu_list error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // 리포트 발송: 기존 메시지를 정적 텍스트로 교체 후, 구독 목록을 새 메시지로 하단에 포스팅
        app.blockAction("menu_report", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                // 1. 기존 메시지를 정적 완료 텍스트로 교체 (버튼 제거)
                update(botToken, channelId, messageTs,
                        List.of(section(s -> s.text(markdownText("📊 리포트가 발송되었습니다.")))));
                // 2. 리포트 발송 (새 메시지)
                reportService.sendReportToUser(user, YearMonth.now());
                // 3. 구독 목록을 새 메시지로 채팅 하단에 포스팅
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                post(botToken, channelId, SlackMessageBuilder.buildSubscriptionList(subs));
            } catch (Exception e) {
                log.error("menu_report error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // 카테고리 선택 화면으로 업데이트 (커스텀 카테고리 목록 포함)
        app.blockAction("menu_add", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                List<String> customCats = subscriptionService.getDistinctCustomCategories(user);
                update(botToken, channelId, messageTs, SlackMessageBuilder.buildCategoryButtons(customCats));
            } catch (Exception e) {
                log.error("menu_add error", e);
            }
            return ctx.ack();
        });

        // hist_{id}: 결제 이력 표시
        app.blockAction(Pattern.compile("hist_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            long subscriptionId = Long.parseLong(actionId.substring("hist_".length()));
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                Subscription sub = subscriptionService.getActiveSubscription(user, subscriptionId);
                var records = paymentRecordService.getHistory(sub);
                update(botToken, channelId, messageTs,
                        SlackMessageBuilder.buildPaymentHistory(sub.getServiceName(), records));
            } catch (Exception e) {
                log.error("hist action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // del_ok_{id}: 삭제 확인(confirm 다이얼로그) 후 실제 삭제
        app.blockAction(Pattern.compile("del_ok_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            long subscriptionId = Long.parseLong(actionId.substring("del_ok_".length()));
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                subscriptionService.delete(user, subscriptionId);
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                update(botToken, channelId, messageTs, SlackMessageBuilder.buildSubscriptionList(subs));
            } catch (Exception e) {
                log.error("del_ok action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // ntf_{id}: 알림 관리 화면 (현재 설정 목록 + 삭제 + 추가)
        app.blockAction(Pattern.compile("ntf_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            long subscriptionId = Long.parseLong(actionId.substring("ntf_".length()));
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                Subscription sub = subscriptionService.getActiveSubscription(user, subscriptionId);
                var currentSettings = notificationService.getByUserAndSubscription(user, sub);
                update(botToken, channelId, messageTs,
                        SlackMessageBuilder.buildNotifyOptions(SubscriptionResponse.from(sub), currentSettings));
            } catch (Exception e) {
                log.error("ntf action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // ntf_ok_{days}_{id}: 알림 저장 후 목록으로
        app.blockAction(Pattern.compile("ntf_ok_(\\d+)_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String rest = actionId.substring("ntf_ok_".length()); // "{days}_{id}"
            int underscoreIdx = rest.indexOf('_');
            int days = Integer.parseInt(rest.substring(0, underscoreIdx));
            long subscriptionId = Long.parseLong(rest.substring(underscoreIdx + 1));
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                Subscription sub = subscriptionService.getActiveSubscription(user, subscriptionId);
                notificationService.addCustomNotification(user, sub, days);
                var currentSettings = notificationService.getByUserAndSubscription(user, sub);
                update(botToken, channelId, messageTs,
                        SlackMessageBuilder.buildNotifyOptions(SubscriptionResponse.from(sub), currentSettings));
            } catch (Exception e) {
                log.error("ntf_ok action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // ntf_del_{settingId}_{subId}: 알림 삭제 후 알림 관리 화면으로
        app.blockAction(Pattern.compile("ntf_del_(\\d+)_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String rest = actionId.substring("ntf_del_".length());
            int underscoreIdx = rest.indexOf('_');
            long settingId = Long.parseLong(rest.substring(0, underscoreIdx));
            long subscriptionId = Long.parseLong(rest.substring(underscoreIdx + 1));
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                notificationService.deleteNotification(user, settingId);
                Subscription sub = subscriptionService.getActiveSubscription(user, subscriptionId);
                var currentSettings = notificationService.getByUserAndSubscription(user, sub);
                update(botToken, channelId, messageTs,
                        SlackMessageBuilder.buildNotifyOptions(SubscriptionResponse.from(sub), currentSettings));
            } catch (Exception e) {
                log.error("ntf_del action error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // back_list: state 초기화 후 목록으로
        app.blockAction("back_list", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                stateStore.remove(userId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                update(botToken, channelId, messageTs, SlackMessageBuilder.buildSubscriptionList(subs));
            } catch (Exception e) {
                log.error("back_list error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // category_{name} 클릭 → 서비스명 입력 요청
        app.blockAction(Pattern.compile("category_(.+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
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
                String prompt = String.format("*%s* 를 선택하셨어요.\n\n서비스 이름을 아래 채팅창에 입력해주세요.\n_예) Netflix, Spotify, GitHub_", catLabel);
                update(botToken, channelId, messageTs,
                        List.of(section(s -> s.text(markdownText(prompt)))));
            } catch (Exception e) {
                log.error("category action error", e);
            }
            return ctx.ack();
        });

        // custom_cat_{idx}: 이전 커스텀 카테고리 선택
        app.blockAction(Pattern.compile("custom_cat_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String categoryName = req.getPayload().getActions().get(0).getValue();

            ConversationState state = new ConversationState();
            state.setStep(ConversationStep.AWAITING_SERVICE_NAME);
            state.setCategoryCode("OTHER");
            state.setCustomCategoryName(categoryName);
            state.setTeamId(teamId);
            stateStore.put(userId, state);

            try {
                String botToken = tokenResolver.getBotToken(teamId);
                String prompt = String.format("*%s* 카테고리를 선택하셨어요.\n\n서비스 이름을 아래 채팅창에 입력해주세요.\n_예) Netflix, Spotify, GitHub_", categoryName);
                update(botToken, channelId, messageTs,
                        List.of(section(s -> s.text(markdownText(prompt)))));
            } catch (Exception e) {
                log.error("custom_cat action error", e);
            }
            return ctx.ack();
        });

        // amount_preset_{value}: 금액 프리셋 버튼 선택
        app.blockAction(Pattern.compile("amount_preset_(\\d+)"), (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String actionId = req.getPayload().getActions().get(0).getActionId();
            BigDecimal amount = new BigDecimal(actionId.substring("amount_preset_".length()));
            ConversationState state = stateStore.get(userId);
            if (state == null || state.getStep() != ConversationStep.AWAITING_AMOUNT) return ctx.ack();
            state.setAmount(amount);
            state.setStep(ConversationStep.AWAITING_BILLING_DAY);
            stateStore.put(userId, state);
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                update(botToken, channelId, messageTs,
                        SlackMessageBuilder.buildBillingDaySelection(state.getServiceName(), amount));
            } catch (Exception e) {
                log.error("amount_preset error", e);
            }
            return ctx.ack();
        });

        // amount_direct: 직접 입력 버튼 → 텍스트 입력 안내
        app.blockAction("amount_direct", (req, ctx) -> {
            ctx.ack();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                update(botToken, channelId, messageTs,
                        List.of(section(s -> s.text(markdownText("금액을 채팅창에 입력해주세요. (예: 15900)")))));
            } catch (Exception e) {
                log.error("amount_direct error", e);
            }
            return ctx.ack();
        });

        // billing_day_select: 결제일 드롭다운 선택 → 구독 생성
        app.blockAction("billing_day_select", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();
            String selectedValue = req.getPayload().getActions().get(0).getSelectedOption().getValue();
            int billingDay = Integer.parseInt(selectedValue);
            ConversationState state = stateStore.get(userId);
            if (state == null || state.getStep() != ConversationStep.AWAITING_BILLING_DAY) return ctx.ack();
            stateStore.remove(userId);
            try {
                String botToken = tokenResolver.getBotToken(teamId);
                User user = userService.getOrCreateUser(userId, teamId, null);
                SubscriptionCategory category = state.getCategoryCode() != null
                        ? SubscriptionCategory.valueOf(state.getCategoryCode())
                        : SubscriptionCategory.OTHER;
                subscriptionService.create(user,
                        SubscriptionCreateRequest.builder()
                                .serviceName(state.getServiceName())
                                .category(category)
                                .customCategoryName(state.getCustomCategoryName())
                                .amount(state.getAmount())
                                .billingDay(billingDay)
                                .billingCycle(BillingCycle.MONTHLY)
                                .build());
                update(botToken, channelId, messageTs,
                        List.of(section(s -> s.text(markdownText(
                                String.format("*%s* 구독을 등록했어요!", state.getServiceName()))))));
                List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
                post(botToken, userId, SlackMessageBuilder.buildSubscriptionList(subs));
            } catch (Exception e) {
                log.error("billing_day_select error", e);
                sendError(userId, teamId, e.getMessage());
            }
            return ctx.ack();
        });

        // new_category → 카테고리명 직접 입력
        app.blockAction("new_category", (req, ctx) -> {
            ctx.ack();
            String userId = req.getPayload().getUser().getId();
            String teamId = req.getPayload().getTeam().getId();
            String channelId = req.getPayload().getChannel().getId();
            String messageTs = req.getPayload().getMessage().getTs();

            ConversationState state = new ConversationState();
            state.setStep(ConversationStep.AWAITING_CUSTOM_CATEGORY);
            state.setTeamId(teamId);
            stateStore.put(userId, state);

            try {
                String botToken = tokenResolver.getBotToken(teamId);
                update(botToken, channelId, messageTs,
                        List.of(section(s -> s.text(markdownText("카테고리 이름을 채팅창에 입력해주세요. (예: 클라우드)")))));
            } catch (Exception e) {
                log.error("new_category action error", e);
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
                    post(botToken, userId, SlackMessageBuilder.buildAmountSelection(text));
                }
                case AWAITING_AMOUNT -> {
                    BigDecimal amount = new BigDecimal(text.replaceAll("[^0-9.]", ""));
                    state.setAmount(amount);
                    state.setStep(ConversationStep.AWAITING_BILLING_DAY);
                    stateStore.put(userId, state);
                    post(botToken, userId, SlackMessageBuilder.buildBillingDaySelection(state.getServiceName(), amount));
                }
                case AWAITING_BILLING_DAY -> {
                    int billingDay = Integer.parseInt(text.trim());
                    stateStore.remove(userId);

                    User user = userService.getOrCreateUser(userId, resolvedTeamId, null);
                    SubscriptionCategory category = state.getCategoryCode() != null
                            ? SubscriptionCategory.valueOf(state.getCategoryCode())
                            : SubscriptionCategory.OTHER;

                    subscriptionService.create(user,
                            SubscriptionCreateRequest.builder()
                                    .serviceName(state.getServiceName())
                                    .category(category)
                                    .customCategoryName(state.getCustomCategoryName())
                                    .amount(state.getAmount())
                                    .billingDay(billingDay)
                                    .billingCycle(BillingCycle.MONTHLY)
                                    .build());

                    sendSubscriptionListOrWelcome(userId, resolvedTeamId);
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

    private void sendSubscriptionListOrWelcome(String userId, String teamId) {
        try {
            String botToken = tokenResolver.getBotToken(teamId);
            User user = userService.getOrCreateUser(userId, teamId, null);
            List<SubscriptionResponse> subs = subscriptionService.listByUser(user);
            post(botToken, userId, SlackMessageBuilder.buildSubscriptionList(subs));
        } catch (Exception e) {
            log.error("sendSubscriptionListOrWelcome error", e);
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

    private void post(String botToken, String channel, List<LayoutBlock> blocks) throws Exception {
        app.client().chatPostMessage(r -> r.token(botToken).channel(channel).blocks(blocks));
    }

    private void postText(String botToken, String channel, String text) throws Exception {
        app.client().chatPostMessage(r -> r.token(botToken).channel(channel).text(text));
    }

    private void update(String botToken, String channel, String ts, List<LayoutBlock> blocks) throws Exception {
        app.client().chatUpdate(r -> r.token(botToken).channel(channel).ts(ts).blocks(blocks).text(" "));
    }

}
