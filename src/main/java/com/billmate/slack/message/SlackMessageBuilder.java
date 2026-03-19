package com.billmate.slack.message;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.block.element.ButtonElement;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.button;

public class SlackMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // ── 메인 메뉴 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildMainMenu() {
        return buildMainMenu(null);
    }

    public static List<LayoutBlock> buildMainMenu(String contextMessage) {
        List<LayoutBlock> blocks = new ArrayList<>();

        String intro = contextMessage != null
                ? contextMessage + "\n\n무엇을 더 도와드릴까요?"
                : "안녕하세요! 무엇을 도와드릴까요? :wave:";

        blocks.add(section(s -> s.text(markdownText(intro))));
        blocks.add(divider());
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("➕  구독 추가")).actionId("menu_add").style("primary")),
                button(b -> b.text(plainText("📋  목록 보기")).actionId("menu_list")),
                button(b -> b.text(plainText("📊  리포트")).actionId("menu_report"))
        ))));
        blocks.add(actions(a -> a.elements(List.of(
                button(b -> b.text(plainText("📜  결제 이력")).actionId("menu_history")),
                button(b -> b.text(plainText("🗑️  구독 삭제")).actionId("menu_delete").style("danger")),
                button(b -> b.text(plainText("🔔  알림 설정")).actionId("menu_notify")),
                button(b -> b.text(plainText("👥  공유 설정")).actionId("menu_share"))
        ))));
        return blocks;
    }

    // ── 구독 목록 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildSubscriptionList(List<SubscriptionResponse> subs) {
        List<LayoutBlock> blocks = new ArrayList<>();

        if (subs.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText(
                    "*📋 구독 목록*\n\n아직 등록된 구독이 없어요.\n*➕ 구독 추가* 버튼으로 첫 구독을 등록해보세요!"))));
            return blocks;
        }

        BigDecimal total = subs.stream()
                .map(SubscriptionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        blocks.add(section(s -> s.text(markdownText(
                String.format("*📋 구독 목록*   _%d개 · 월 ₩%s_", subs.size(), formatAmount(total))))));
        blocks.add(divider());

        for (SubscriptionResponse sub : subs) {
            String text = String.format("%s  *%s*   `₩%s`\n매월 *%d일* 결제  ·  %s",
                    categoryEmoji(sub.getCategory()),
                    sub.getServiceName(),
                    formatAmount(sub.getAmount()),
                    sub.getBillingDay(),
                    sub.getCategoryDisplay());
            blocks.add(section(s -> s.text(markdownText(text))));
        }
        return blocks;
    }

    // ── 구독 추가 완료 ───────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildSubscriptionAdded(SubscriptionResponse sub) {
        String text = String.format("✅  *%s* 구독을 등록했어요!\n매월 *%d일* 결제  ·  *₩%s*  ·  %s",
                sub.getServiceName(),
                sub.getBillingDay(),
                formatAmount(sub.getAmount()),
                sub.getCategoryDisplay());
        return List.of(section(s -> s.text(markdownText(text))));
    }

    // ── 카테고리 버튼 ────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildCategoryButtons() {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(section(s -> s.text(markdownText("*➕ 구독 추가*\n어떤 종류의 구독인가요?"))));
        blocks.add(divider());

        List<ButtonElement> allButtons = new ArrayList<>();
        for (SubscriptionCategory cat : SubscriptionCategory.values()) {
            String label = categoryEmoji(cat) + "  " + categoryLabel(cat);
            String actionId = "category_" + cat.name();
            allButtons.add(button(b -> b.text(plainText(label)).actionId(actionId)));
        }
        allButtons.add(button(b -> b.text(plainText("✏️  직접 입력")).actionId("new_category")));

        int chunkSize = 4;
        for (int i = 0; i < allButtons.size(); i += chunkSize) {
            List<ButtonElement> chunk = List.copyOf(allButtons.subList(i, Math.min(i + chunkSize, allButtons.size())));
            blocks.add(actions(a -> a.elements(new ArrayList<>(chunk))));
        }
        return blocks;
    }

    // ── 구독 선택 버튼 (이력/삭제용) ─────────────────────────────────────────────

    public static List<LayoutBlock> buildSubscriptionButtons(List<SubscriptionResponse> subs, String actionPrefix) {
        List<LayoutBlock> blocks = new ArrayList<>();

        String title = actionPrefix.equals("history") ? "📜  결제 이력" : "🗑️  구독 삭제";
        String guide = actionPrefix.equals("history") ? "이력을 확인할 구독을 선택해주세요." : "삭제할 구독을 선택해주세요.";

        if (subs.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText("등록된 구독이 없어요."))));
            return blocks;
        }

        blocks.add(section(s -> s.text(markdownText(String.format("*%s*\n%s", title, guide)))));

        List<ButtonElement> buttons = new ArrayList<>();
        for (SubscriptionResponse sub : subs) {
            String label = categoryEmoji(sub.getCategory()) + "  " + sub.getServiceName();
            String actionId = actionPrefix + "_sub_" + sub.getId();
            ButtonElement btn = button(b -> b.text(plainText(label)).actionId(actionId));
            if (actionPrefix.equals("delete")) {
                buttons.add(button(b -> b.text(plainText(label)).actionId(actionId).style("danger")));
            } else {
                buttons.add(btn);
            }
        }

        int chunkSize = 5;
        for (int i = 0; i < buttons.size(); i += chunkSize) {
            List<ButtonElement> chunk = List.copyOf(buttons.subList(i, Math.min(i + chunkSize, buttons.size())));
            blocks.add(actions(a -> a.elements(new ArrayList<>(chunk))));
        }
        return blocks;
    }

    // ── 결제 이력 ────────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildPaymentHistory(List<PaymentRecord> records) {
        List<LayoutBlock> blocks = new ArrayList<>();

        if (records.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText("*📜 결제 이력*\n\n아직 결제 이력이 없어요."))));
            return blocks;
        }

        BigDecimal total = records.stream()
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        blocks.add(section(s -> s.text(markdownText(
                String.format("*📜 결제 이력*   _%d건 · 총 ₩%s_", records.size(), formatAmount(total))))));
        blocks.add(divider());

        for (PaymentRecord record : records) {
            String statusEmoji = switch (record.getStatus()) {
                case PAID -> "✅";
                case SKIPPED -> "⏭️";
                case CANCELLED -> "❌";
            };
            String text = String.format("%s  `%s`   *₩%s*",
                    statusEmoji,
                    record.getBilledAt().format(DATE_FMT),
                    formatAmount(record.getAmount()));
            blocks.add(section(s -> s.text(markdownText(text))));
        }
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

        String text = String.format(
                "*🔔 결제 예정 알림*\n\n*%s* 결제일이 *%s*입니다.\n결제 금액: *₩%s*",
                serviceName, dayText, formatAmount(amount));

        if (cancelUrl != null && !cancelUrl.isBlank()) {
            text += String.format("\n\n구독 해지를 원하시면 <%s|여기>를 눌러주세요.", cancelUrl);
        }

        final String finalText = text;
        return List.of(section(s -> s.text(markdownText(finalText))));
    }

    // ── 월간 리포트 ──────────────────────────────────────────────────────────────

    public static List<LayoutBlock> buildMonthlyReport(String summary, String chartUrl) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(section(s -> s.text(markdownText("*📊 월간 구독 리포트*"))));
        blocks.add(divider());
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
            case OTT -> "📺";
            case MUSIC_STREAMING -> "🎵";
            case CLOUD_STORAGE -> "☁️";
            case PRODUCTIVITY -> "📋";
            case DEVELOPER_TOOL -> "💻";
            case AI_TOOL -> "🤖";
            case GAME -> "🎮";
            case FITNESS -> "💪";
            case NEWS -> "📰";
            case EDUCATION -> "📚";
            case OTHER -> "📦";
        };
    }

    public static String categoryLabel(SubscriptionCategory cat) {
        return switch (cat) {
            case OTT -> "동영상";
            case MUSIC_STREAMING -> "음악";
            case CLOUD_STORAGE -> "클라우드";
            case PRODUCTIVITY -> "생산성";
            case DEVELOPER_TOOL -> "개발 도구";
            case AI_TOOL -> "AI";
            case GAME -> "게임";
            case FITNESS -> "피트니스";
            case NEWS -> "뉴스";
            case EDUCATION -> "교육";
            case OTHER -> "기타";
        };
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
