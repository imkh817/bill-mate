package com.billmate.slack.message;

import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SlackMessageBuilderTest {

    @Test
    @DisplayName("UC-MB1: buildSubscriptionList 빈 리스트 → '등록된 구독이 없습니다' 포함")
    void buildSubscriptionList_empty() {
        List<LayoutBlock> blocks = SlackMessageBuilder.buildSubscriptionList(List.of());

        String allText = extractAllText(blocks);
        assertThat(allText).contains("등록된 구독이 없습니다");
    }

    @Test
    @DisplayName("UC-MB2: buildSubscriptionList 1개 구독 → 서비스명, 금액, 결제일 포함")
    void buildSubscriptionList_oneSubscription() {
        SubscriptionResponse sub = SubscriptionResponse.builder()
                .id(1L)
                .serviceName("Netflix")
                .category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900"))
                .currency("KRW")
                .billingDay(25)
                .billingCycle(BillingCycle.MONTHLY)
                .startedAt(LocalDate.now())
                .build();

        List<LayoutBlock> blocks = SlackMessageBuilder.buildSubscriptionList(List.of(sub));

        String allText = extractAllText(blocks);
        assertThat(allText).contains("Netflix");
        assertThat(allText).contains("15,900");
        assertThat(allText).contains("25");
    }

    @Test
    @DisplayName("UC-MB3: buildNotification D-0 → '오늘' 문구 포함")
    void buildNotification_dDay() {
        List<LayoutBlock> blocks = SlackMessageBuilder.buildNotification(
                "Netflix", 0, new BigDecimal("15900"), "KRW", null);

        assertThat(extractAllText(blocks)).contains("오늘");
    }

    @Test
    @DisplayName("UC-MB4: buildNotification D-3 → '3일 후' 문구 포함")
    void buildNotification_threeDaysBefore() {
        List<LayoutBlock> blocks = SlackMessageBuilder.buildNotification(
                "Netflix", 3, new BigDecimal("15900"), "KRW", null);

        assertThat(extractAllText(blocks)).contains("3일 후");
    }

    @Test
    @DisplayName("UC-MB5: buildNotification cancelUrl 있으면 해지 링크 포함")
    void buildNotification_withCancelUrl() {
        List<LayoutBlock> blocks = SlackMessageBuilder.buildNotification(
                "Netflix", 3, new BigDecimal("15900"), "KRW", "https://cancel.example.com");

        assertThat(extractAllText(blocks)).contains("https://cancel.example.com");
    }

    @Test
    @DisplayName("UC-MB6: buildMonthlyReport chartUrl null → image 블록 없음")
    void buildMonthlyReport_noChartUrl() {
        List<LayoutBlock> blocks = SlackMessageBuilder.buildMonthlyReport("리포트 내용", null);

        long imageBlockCount = blocks.stream()
                .filter(b -> b instanceof com.slack.api.model.block.ImageBlock)
                .count();
        assertThat(imageBlockCount).isEqualTo(0);
    }

    @Test
    @DisplayName("UC-MB7: buildError → ':x: ' 접두사 포함")
    void buildError_prefixX() {
        List<LayoutBlock> blocks = SlackMessageBuilder.buildError("오류 메시지");

        assertThat(extractAllText(blocks)).contains(":x: ");
    }

    private String extractAllText(List<LayoutBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (LayoutBlock block : blocks) {
            if (block instanceof SectionBlock section && section.getText() != null) {
                sb.append(section.getText().getText());
            }
            if (block instanceof com.slack.api.model.block.HeaderBlock header
                    && header.getText() != null) {
                sb.append(header.getText().getText());
            }
        }
        return sb.toString();
    }
}
