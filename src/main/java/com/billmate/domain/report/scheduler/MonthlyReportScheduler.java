package com.billmate.domain.report.scheduler;

import com.billmate.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private final ReportService reportService;

    @Scheduled(cron = "0 0 9 1 * *", zone = "Asia/Seoul")
    public void sendMonthlyReports() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("Sending monthly reports for {}", lastMonth);
        reportService.sendMonthlyReportToAll(lastMonth);
    }
}
