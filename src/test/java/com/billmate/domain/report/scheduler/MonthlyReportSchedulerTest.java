package com.billmate.domain.report.scheduler;

import com.billmate.domain.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyReportSchedulerTest {

    @Mock
    ReportService reportService;

    @InjectMocks
    MonthlyReportScheduler scheduler;

    @Test
    @DisplayName("UC-MS1: sendMonthlyReports() → 전월 YearMonth로 sendMonthlyReportToAll 호출")
    void sendMonthlyReports_callsWithLastMonth() {
        YearMonth fixedNow = YearMonth.of(2024, 3);
        try (MockedStatic<YearMonth> mockedYearMonth = mockStatic(YearMonth.class, CALLS_REAL_METHODS)) {
            mockedYearMonth.when(YearMonth::now).thenReturn(fixedNow);

            scheduler.sendMonthlyReports();
        }

        then(reportService).should(times(1)).sendMonthlyReportToAll(YearMonth.of(2024, 2));
    }
}
