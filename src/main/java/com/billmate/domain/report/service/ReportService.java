package com.billmate.domain.report.service;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.entity.PaymentStatus;
import com.billmate.domain.payment.repository.PaymentRecordRepository;
import com.billmate.domain.report.dto.MonthlyReportDto;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.repository.UserRepository;
import com.billmate.slack.install.InstallationTokenResolver;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ChartService chartService;
    private final App slackApp;
    private final InstallationTokenResolver tokenResolver;

    @Transactional(readOnly = true)
    public MonthlyReportDto buildReport(User user, YearMonth targetMonth) {
        LocalDate from = targetMonth.atDay(1);
        LocalDate to = targetMonth.atEndOfMonth();

        List<PaymentRecord> records = paymentRecordRepository
                .findByUserIdAndBilledAtBetween(user.getId(), from, to);

        BigDecimal total = records.stream()
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<SubscriptionCategory, BigDecimal> categoryTotals = records.stream()
                .collect(Collectors.groupingBy(
                        pr -> pr.getSubscription().getCategory(),
                        Collectors.reducing(BigDecimal.ZERO, PaymentRecord::getAmount, BigDecimal::add)
                ));

        String chartUrl = chartService.generatePieChartUrl(categoryTotals);

        return MonthlyReportDto.builder()
                .targetMonth(targetMonth)
                .totalAmount(total)
                .subscriptionCount((int) records.stream()
                        .map(pr -> pr.getSubscription().getId())
                        .distinct()
                        .count())
                .categoryTotals(categoryTotals)
                .chartUrl(chartUrl)
                .build();
    }

    public void sendMonthlyReportToAll(YearMonth targetMonth) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                MonthlyReportDto report = buildReport(user, targetMonth);
                String botToken = tokenResolver.getBotToken(user.getSlackWorkspaceId());
                slackApp.client().chatPostMessage(r -> r
                        .token(botToken)
                        .channel(user.getSlackUserId())
                        .blocks(SlackMessageBuilder.buildMonthlyReport(
                                report.toSlackSummary(), report.getChartUrl())));
            } catch (Exception e) {
                log.error("Failed to send monthly report to user {}", user.getSlackUserId(), e);
            }
        }
    }

    public void sendReportToUser(User user, YearMonth targetMonth) throws Exception {
        MonthlyReportDto report = buildReport(user, targetMonth);
        String botToken = tokenResolver.getBotToken(user.getSlackWorkspaceId());
        slackApp.client().chatPostMessage(r -> r
                .token(botToken)
                .channel(user.getSlackUserId())
                .blocks(SlackMessageBuilder.buildMonthlyReport(
                        report.toSlackSummary(), report.getChartUrl())));
    }
}
