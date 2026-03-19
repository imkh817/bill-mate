package com.billmate.domain.report.service;

import com.billmate.domain.subscription.entity.SubscriptionCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ChartServiceTest {

    private final ChartService chartService = new ChartService();

    @Test
    @DisplayName("UC-CS1: 빈 Map → null 반환")
    void generatePieChartUrl_emptyMap_returnsNull() {
        String url = chartService.generatePieChartUrl(Map.of());

        assertThat(url).isNull();
    }

    @Test
    @DisplayName("UC-CS2: 단일 카테고리 → URL에 'quickchart.io' 포함")
    void generatePieChartUrl_singleCategory_containsQuickChartDomain() {
        Map<SubscriptionCategory, BigDecimal> amounts = Map.of(
                SubscriptionCategory.OTT, new BigDecimal("15900")
        );

        String url = chartService.generatePieChartUrl(amounts);

        assertThat(url).isNotNull();
        assertThat(url).contains("quickchart.io");
    }

    @Test
    @DisplayName("UC-CS3: URL에 카테고리명(OTT) label이 포함됨")
    void generatePieChartUrl_containsCategoryLabel() {
        Map<SubscriptionCategory, BigDecimal> amounts = Map.of(
                SubscriptionCategory.OTT, new BigDecimal("15900")
        );

        String url = chartService.generatePieChartUrl(amounts);

        assertThat(url).contains("OTT");
    }
}
