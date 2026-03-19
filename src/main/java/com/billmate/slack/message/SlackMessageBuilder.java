package com.billmate.slack.message;

import com.billmate.domain.notification.entity.NotificationSetting;
import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.StaticSelectElement;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.button;

public class SlackMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // ── 온보딩 (최초 설치 시) ────────────────────────────────────────────────────

    public static List<LayoutBlock> buildOnboarding() {
        return List.of(
                section(s -> s.text(markdownText(
                        "👋 안녕하세요! *bill-mate* 에 오신 것을 환영합니다 🎉\n" +
                        "구독 서비스를 등록하고 결제일 알림과 월간 리포트를 Slack DM에서 관리해보세요."))),
                divider(),
                section(s -> s.text(markdownText(
                        "*Step 1. 구독 추가*\n" +
                        "이 메시지 하단의 *[➕ 구독 추가]* 버튼을 눌러\nNetflix, Spotify 등 구독 서비스를 등록해보세요."))),
                section(s -> s.text(markdownText(
                        "*Step 2. 리포트 확인*\n" +
                        "구독 목록 하단의 *[📊 리포트]* 버튼을 눌러\n월간 지출 리포트와 카테고리별 차트를 확인해보세요."))),
                section(s -> s.text(markdownText(
                        "*Step 3. 알림 설정*\n" +
                        "구독 항목의 *[관리 ▼]* → 알림 설정에서\n결제 며칠 전에 알림을 받을지 설정해보세요."))),
                divider(),
                section(s -> s.text(markdownText(
                        "🧹 대화 기록이 지저분해지면 `/billmate clear` 로 정리할 수 있어요.\n" +
                        "궁금한 점은 `/billmate help` 를 입력해주세요!"))),
                divider(),
                actions(a -> a.elements(List.of(
                        button(b -> b.text(plainText("➕  구독 추가")).actionId("menu_add").style("primary"))
                )))
        );
    }

    // ── 웰컴 (구독 없을 때) ─────────────────────────────────────────────────────

    public static List<LayoutBlock> buildWelcome() {
        return List.of(
                section(s -> s.text(markdownText("안녕하세요! 👋\n아직 등록된 구독이 없어요."))),
                section(s -> s.text(markdownText("첫 구독을 추가해보세요."))),
                actions(a -> a.elements(List.of(
                        button(b -> b.text(plainText("➕  구독 추가")).actionId("menu_add").style("primary"))
                )))
        );
    }

    // ── 구독 목록 ─────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildSubscriptionList(List<SubscriptionResponse> subs) {
        if (subs.isEmpty()) {
            return buildWelcome();
        }

        List<LayoutBlock> blocks = new ArrayList<>();

        BigDecimal total = subs.stream()
                .map(SubscriptionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        blocks.add(header(h -> h.text(plainText("📋 내 구독"))));
        blocks.add(context(c -> c.elements(
                List.<ContextBlockElement>of(
                        markdownText(String.format("_%d개 · 월 ₩%s_", subs.size(), formatAmount(total)))))));
        blocks.add(divider());

        for (SubscriptionResponse sub : subs) {
            final long subId = sub.getId();
            final int billingDay = sub.getBillingDay();
            final String categoryDisplay = sub.getCategoryDisplay();

            String itemText = String.format("%s  *%s*   ₩%s",
                    categoryEmoji(sub.getCategory()),
                    sub.getServiceName(),
                    formatAmount(sub.getAmount()));

            List<OptionObject> menuOptions = List.of(
                    OptionObject.builder().text(plainText("📜  결제 이력")).value("hist_" + subId).build(),
                    OptionObject.builder().text(plainText("🔔  알림 설정")).value("ntf_" + subId).build(),
                    OptionObject.builder().text(plainText("🗑️  삭제")).value("del_" + subId).build()
            );

            blocks.add(section(s -> s
                    .text(markdownText(itemText))
                    .accessory(StaticSelectElement.builder()
                            .actionId("sub_menu")
                            .placeholder(plainText("관리 ▼"))
                            .options(menuOptions)
                            .build())));
            blocks.add(context(c -> c.elements(
                    List.<ContextBlockElement>of(
                            markdownText(String.format("매월 *%d일* 결제  ·  %s", billingDay, categoryDisplay))))));
            blocks.add(divider());
        }

        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("➕  구독 추가")).actionId("menu_add").style("primary")),
                button(b -> b.text(plainText("📊  리포트")).actionId("menu_report"))
        ))));
        return blocks;
    }

    // ── 삭제 확인 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildDeleteConfirm(SubscriptionResponse sub) {
        return List.of(
                section(s -> s.text(markdownText(
                        String.format("⚠️ *%s* 구독을 삭제할까요?", sub.getServiceName())))),
                context(c -> c.elements(
                        List.<ContextBlockElement>of(
                                markdownText(String.format("매월 *%d일* 결제  ·  ₩%s",
                                        sub.getBillingDay(), formatAmount(sub.getAmount())))))),
                actions(a -> a.elements(List.of(
                        button(b -> b.text(plainText("🗑️  삭제")).actionId("del_ok_" + sub.getId()).style("danger")),
                        button(b -> b.text(plainText("← 취소")).actionId("back_list"))
                )))
        );
    }

    // ── 알림 관리 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildNotifyOptions(SubscriptionResponse sub,
                                                       List<NotificationSetting> currentSettings) {
        long subId = sub.getId();
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.add(header(h -> h.text(plainText("🔔 알림 설정"))));
        blocks.add(section(s -> s.text(markdownText(
                String.format("*%s*   ₩%s", sub.getServiceName(), formatAmount(sub.getAmount()))))));
        blocks.add(context(c -> c.elements(
                List.<ContextBlockElement>of(
                        markdownText(String.format("매월 *%d일* 결제", sub.getBillingDay()))))));
        blocks.add(divider());

        if (currentSettings.isEmpty()) {
            blocks.add(context(c -> c.elements(
                    List.<ContextBlockElement>of(markdownText("등록된 알림이 없어요.")))));
        } else {
            currentSettings.stream()
                    .sorted(Comparator.comparingInt(NotificationSetting::getDaysBeforeBilling).reversed())
                    .forEach(ns -> {
                        String label = daysLabel(ns.getDaysBeforeBilling());
                        blocks.add(section(s -> s
                                .text(markdownText(label))
                                .accessory(button(b -> b
                                        .text(plainText("🗑️ 삭제"))
                                        .actionId("ntf_del_" + ns.getId() + "_" + subId)
                                        .style("danger")))));
                    });
        }

        Set<Integer> existingDays = currentSettings.stream()
                .map(NotificationSetting::getDaysBeforeBilling)
                .collect(Collectors.toSet());
        List<Integer> availableDays = List.of(0, 1, 3, 7).stream()
                .filter(d -> !existingDays.contains(d))
                .toList();

        if (!availableDays.isEmpty()) {
            blocks.add(divider());
            List<BlockElement> addButtons = availableDays.stream()
                    .map(d -> (BlockElement) button(b -> b.text(plainText(daysLabel(d))).actionId("ntf_ok_" + d + "_" + subId)))
                    .collect(Collectors.toList());
            blocks.add(actions(a -> a.elements(addButtons)));
        }

        blocks.add(divider());
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("← 목록으로")).actionId("back_list"))
        ))));

        return blocks;
    }

    private static String daysLabel(int days) {
        return switch (days) {
            case 0 -> "당일";
            case 1 -> "1일 전";
            case 3 -> "3일 전";
            case 7 -> "7일 전";
            default -> days + "일 전";
        };
    }

    // ── 구독 추가 완료 ───────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildSubscriptionAdded(SubscriptionResponse sub) {
        String text = String.format("*%s* 구독을 등록했어요!\n매월 *%d일* 결제  ·  *₩%s*  ·  %s",
                sub.getServiceName(),
                sub.getBillingDay(),
                formatAmount(sub.getAmount()),
                sub.getCategoryDisplay());
        return List.of(section(s -> s.text(markdownText(text))));
    }

    // ── 금액 선택 (프리셋 버튼) ──────────────────────────────────────────────────

    public static List<LayoutBlock> buildAmountSelection(String serviceName) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(header(h -> h.text(plainText("💳 금액 선택"))));
        blocks.add(section(s -> s.text(markdownText(
                String.format("*%s* 월 결제 금액을 선택해주세요.", serviceName)))));
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("₩9,900")).actionId("amount_preset_9900")),
                button(b -> b.text(plainText("₩13,900")).actionId("amount_preset_13900")),
                button(b -> b.text(plainText("₩14,900")).actionId("amount_preset_14900")),
                button(b -> b.text(plainText("₩17,000")).actionId("amount_preset_17000")),
                button(b -> b.text(plainText("₩19,900")).actionId("amount_preset_19900"))
        ))));
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("₩22,000")).actionId("amount_preset_22000")),
                button(b -> b.text(plainText("₩29,000")).actionId("amount_preset_29000")),
                button(b -> b.text(plainText("✏️  직접 입력")).actionId("amount_direct"))
        ))));
        return blocks;
    }

    // ── 결제일 선택 (드롭다운) ───────────────────────────────────────────────────

    public static List<LayoutBlock> buildBillingDaySelection(String serviceName, BigDecimal amount) {
        List<OptionObject> options = new ArrayList<>();
        for (int day = 1; day <= 31; day++) {
            final int d = day;
            options.add(OptionObject.builder()
                    .text(plainText(d + "일"))
                    .value(String.valueOf(d))
                    .build());
        }

        return List.of(
                header(h -> h.text(plainText("📅 결제일 설정"))),
                section(s -> s
                        .text(markdownText(String.format("*%s*  ·  ₩%s", serviceName, formatAmount(amount))))
                        .accessory(StaticSelectElement.builder()
                                .actionId("billing_day_select")
                                .placeholder(plainText("날짜 선택 ▼"))
                                .options(options)
                                .build())),
                context(c -> c.elements(
                        List.<ContextBlockElement>of(markdownText("매월 몇 일에 결제되나요?"))))
        );
    }

    // ── 카테고리 버튼 ────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildCategoryButtons(List<String> customCategories) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(header(h -> h.text(plainText("➕ 구독 추가"))));
        blocks.add(section(s -> s.text(markdownText("어떤 종류의 구독인가요?"))));
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("🎬  동영상")).actionId("category_OTT")),
                button(b -> b.text(plainText("🎵  음악")).actionId("category_MUSIC_STREAMING")),
                button(b -> b.text(plainText("✨  AI")).actionId("category_AI_TOOL")),
                button(b -> b.text(plainText("📝  직접 입력")).actionId("new_category"))
        ))));

        if (customCategories != null && !customCategories.isEmpty()) {
            int chunkSize = 5;
            for (int i = 0; i < customCategories.size(); i += chunkSize) {
                List<String> chunk = customCategories.subList(i, Math.min(i + chunkSize, customCategories.size()));
                List<ButtonElement> buttons = new ArrayList<>();
                for (int j = 0; j < chunk.size(); j++) {
                    String cat = chunk.get(j);
                    final int idx = i + j;
                    buttons.add(button(b -> b.text(plainText(cat)).actionId("custom_cat_" + idx).value(cat)));
                }
                blocks.add(actions(a -> a.elements(new ArrayList<>(buttons))));
            }
        }

        blocks.add(divider());
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("← 취소")).actionId("back_list"))
        ))));
        return blocks;
    }

    // ── 결제 이력 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildPaymentHistory(String serviceName, List<PaymentRecord> records) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(header(h -> h.text(plainText("📜 결제 이력"))));

        if (records.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText(
                    String.format("*%s*   아직 결제 이력이 없어요.", serviceName)))));
            blocks.add(actions(a -> a.elements(List.of(
                    button(b -> b.text(plainText("← 목록으로")).actionId("back_list"))
            ))));
            return blocks;
        }

        BigDecimal total = records.stream()
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        blocks.add(section(s -> s.text(markdownText(
                String.format("*%s*   _%d건 · 총 ₩%s_",
                        serviceName, records.size(), formatAmount(total))))));
        blocks.add(divider());

        for (PaymentRecord record : records) {
            String statusEmoji = switch (record.getStatus()) {
                case PAID -> "💳";
                case SKIPPED -> "⏭️";
                case CANCELLED -> "❌";
            };
            String text = String.format("%s  `%s`   *₩%s*",
                    statusEmoji,
                    record.getBilledAt().format(DATE_FMT),
                    formatAmount(record.getAmount()));
            blocks.add(section(s -> s.text(markdownText(text))));
        }

        blocks.add(divider());
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("← 목록으로")).actionId("back_list"))
        ))));
        return blocks;
    }

    // ── 공유 멤버 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildSharedMembers(List<SharedAccount> members) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(section(s -> s.text(markdownText("*👥 공유 멤버*"))));
        blocks.add(divider());

        if (members.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText("공유 멤버가 없어요."))));
            return blocks;
        }

        for (SharedAccount member : members) {
            String split = member.getSplitAmount() != null
                    ? "   `₩" + formatAmount(member.getSplitAmount()) + "`"
                    : "";
            String text = String.format("<@%s>%s", member.getMemberSlackUserId(), split);
            blocks.add(section(s -> s.text(markdownText(text))));
        }
        return blocks;
    }

    // ── 알림 ─────────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildNotification(String serviceName, int daysBeforeBilling,
                                                       BigDecimal amount, String currency, String cancelUrl) {
        String dayText = switch (daysBeforeBilling) {
            case 0 -> "오늘";
            case 1 -> "내일";
            default -> daysBeforeBilling + "일 후";
        };

        String contextText = "결제 금액  *₩" + formatAmount(amount) + "*";
        if (cancelUrl != null && !cancelUrl.isBlank()) {
            contextText += String.format("   <%s|구독 해지>", cancelUrl);
        }
        final String finalContextText = contextText;

        return List.of(
                section(s -> s.text(markdownText("🔔 *결제 예정 알림*"))),
                section(s -> s.text(markdownText(
                        String.format("*%s* 결제일이 *%s*입니다.", serviceName, dayText)))),
                context(c -> c.elements(
                        List.<ContextBlockElement>of(markdownText(finalContextText))))
        );
    }

    // ── 월간 리포트 ──────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildMonthlyReport(String summary, String chartUrl) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(header(h -> h.text(plainText("📊 월간 구독 리포트"))));
        blocks.add(section(s -> s.text(markdownText(summary))));
        if (chartUrl != null && !chartUrl.isBlank()) {
            blocks.add(image(i -> i
                    .imageUrl(chartUrl)
                    .altText("카테고리별 구독 지출 파이차트")));
        }
        return blocks;
    }

    // ── 도움말 ───────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildHelp() {
        String helpText = """
                *📖 bill-mate 사용법*

                구독 관리는 앱에게 직접 메시지를 보내거나, 슬래시 커맨드로도 사용할 수 있어요.

                *슬래시 커맨드*
                `/billmate add "서비스명" 카테고리 금액 결제일` — 구독 추가
                `/billmate list` — 구독 목록
                `/billmate delete [id]` — 구독 삭제
                `/billmate notify [id] [days]` — 알림 설정
                `/billmate report` — 월간 리포트
                `/billmate history [id]` — 결제 이력
                `/billmate share add [id] [@user] [금액]` — 공유 추가
                `/billmate clear` — DM 대화 기록 정리
                `/billmate seed [months]` — 테스트용 결제 이력 생성 (기본 6개월)
                """;
        return List.of(section(s -> s.text(markdownText(helpText))));
    }

    // ── 에러 ─────────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildError(String message) {
        return List.of(section(s -> s.text(markdownText(":x:  " + message))));
    }

    // ── 카테고리 유틸 ────────────────────────────────────────────────────────────

    public static String categoryEmoji(SubscriptionCategory cat) {
        return switch (cat) {
            case OTT -> "🎬";
            case MUSIC_STREAMING -> "🎵";
            case CLOUD_STORAGE -> "🗄️";
            case PRODUCTIVITY -> "🗂️";
            case DEVELOPER_TOOL -> "🛠️";
            case AI_TOOL -> "✨";
            case GAME -> "🕹️";
            case FITNESS -> "🏃";
            case NEWS -> "🗞️";
            case EDUCATION -> "🎓";
            case OTHER -> "🔖";
        };
    }

    public static String categoryLabel(SubscriptionCategory cat) {
        return cat.displayName();
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
