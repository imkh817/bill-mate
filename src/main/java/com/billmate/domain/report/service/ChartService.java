package com.billmate.domain.report.service;

import com.billmate.domain.subscription.entity.SubscriptionCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private static final String QUICKCHART_BASE = "https://quickchart.io/chart";

    public String generatePieChartUrl(Map<SubscriptionCategory, BigDecimal> categoryAmounts) {
        if (categoryAmounts.isEmpty()) {
            return null;
        }

        String labels = categoryAmounts.keySet().stream()
                .map(cat -> "\"" + cat.displayName() + "\"")
                .collect(Collectors.joining(","));

        String data = categoryAmounts.values().stream()
                .map(BigDecimal::toPlainString)
                .collect(Collectors.joining(","));

        String chartConfig = String.format("""
                {
                  "type": "pie",
                  "data": {
                    "labels": [%s],
                    "datasets": [{
                      "data": [%s]
                    }]
                  },
                  "options": {
                    "title": {
                      "display": true,
                      "text": "카테고리별 구독 지출"
                    },
                    "legend": {
                      "position": "right"
                    }
                  }
                }
                """, labels, data);

        String encoded = URLEncoder.encode(chartConfig, StandardCharsets.UTF_8);
        String url = QUICKCHART_BASE + "?c=" + encoded + "&width=500&height=400";

        // QuickChart URL limit is around 16KB; use short URL if needed
        if (url.length() > 2000) {
            log.warn("Chart URL too long ({}), consider using POST API", url.length());
        }

        return url;
    }
}
