package com.billmate.domain.report.dto;

import com.billmate.domain.subscription.entity.SubscriptionCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

@Getter
@Builder
public class MonthlyReportDto {
    private YearMonth targetMonth;
    private BigDecimal totalAmount;
    private int subscriptionCount;
    private Map<SubscriptionCategory, BigDecimal> categoryTotals;
    private String chartUrl;

    public String toSlackSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*%d년 %d월 구독 리포트*\n\n",
                targetMonth.getYear(), targetMonth.getMonthValue()));
        sb.append(String.format("• 총 지출: *%,.0f원*\n", totalAmount));
        sb.append(String.format("• 활성 구독 수: *%d개*\n\n", subscriptionCount));
        sb.append("*카테고리별 지출*\n");
        categoryTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> sb.append(String.format("• %s: %,.0f원\n",
                        entry.getKey().name(), entry.getValue())));
        return sb.toString();
    }
}
