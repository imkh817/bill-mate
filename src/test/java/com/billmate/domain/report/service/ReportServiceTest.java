package com.billmate.domain.report.service;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.entity.PaymentStatus;
import com.billmate.domain.payment.repository.PaymentRecordRepository;
import com.billmate.domain.report.dto.MonthlyReportDto;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.repository.UserRepository;
import com.billmate.slack.install.InstallationTokenResolver;
import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PaymentRecordRepository paymentRecordRepository;

    @Mock
    ChartService chartService;

    @Mock
    App slackApp;

    @Mock
    InstallationTokenResolver tokenResolver;

    @InjectMocks
    ReportService reportService;

    private User user;
    private Subscription subscription;
    private MethodsClient methodsClient;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder().slackUserId("U001").slackWorkspaceId("T001").displayName("Alice").build();
        subscription = Subscription.builder()
                .user(user).serviceName("Netflix").category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900")).billingDay(25)
                .billingCycle(BillingCycle.MONTHLY).isActive(true).startedAt(LocalDate.now()).build();

        methodsClient = mock(MethodsClient.class);
        given(slackApp.client()).willReturn(methodsClient);
        given(tokenResolver.getBotToken(anyString())).willReturn("xoxb-test");
        given(methodsClient.chatPostMessage(ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()))
                .willReturn(new ChatPostMessageResponse());
    }

    @Test
    @DisplayName("UC-RS1: buildReport — 결제 이력 없으면 totalAmount=0, subscriptionCount=0")
    void buildReport_noRecords_zeroTotals() {
        given(paymentRecordRepository.findByUserIdAndBilledAtBetween(any(), any(), any()))
                .willReturn(List.of());
        given(chartService.generatePieChartUrl(any())).willReturn(null);

        MonthlyReportDto report = reportService.buildReport(user, YearMonth.of(2024, 1));

        assertThat(report.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getSubscriptionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("UC-RS2: buildReport — 카테고리별 집계 sum 검증")
    void buildReport_withRecords_categoryTotals() {
        PaymentRecord r1 = PaymentRecord.builder().subscription(subscription)
                .amount(new BigDecimal("15900")).billedAt(LocalDate.of(2024, 1, 25))
                .status(PaymentStatus.PAID).build();
        PaymentRecord r2 = PaymentRecord.builder().subscription(subscription)
                .amount(new BigDecimal("9900")).billedAt(LocalDate.of(2024, 1, 15))
                .status(PaymentStatus.PAID).build();
        given(paymentRecordRepository.findByUserIdAndBilledAtBetween(any(), any(), any()))
                .willReturn(List.of(r1, r2));
        given(chartService.generatePieChartUrl(any())).willReturn(null);

        MonthlyReportDto report = reportService.buildReport(user, YearMonth.of(2024, 1));

        assertThat(report.getTotalAmount()).isEqualByComparingTo(new BigDecimal("25800"));
        assertThat(report.getCategoryTotals()).containsKey(SubscriptionCategory.OTT);
        assertThat(report.getCategoryTotals().get(SubscriptionCategory.OTT))
                .isEqualByComparingTo(new BigDecimal("25800"));
    }

    @Test
    @DisplayName("UC-RS3: sendMonthlyReportToAll — user 수만큼 chatPostMessage 호출 & tokenResolver 사용")
    void sendMonthlyReportToAll_callsPostForEachUser() throws Exception {
        User user2 = User.builder().slackUserId("U002").slackWorkspaceId("T002").displayName("Bob").build();
        given(userRepository.findAll()).willReturn(List.of(user, user2));
        given(paymentRecordRepository.findByUserIdAndBilledAtBetween(any(), any(), any()))
                .willReturn(List.of());
        given(chartService.generatePieChartUrl(any())).willReturn(null);

        reportService.sendMonthlyReportToAll(YearMonth.of(2024, 1));

        then(tokenResolver).should(times(1)).getBotToken("T001");
        then(tokenResolver).should(times(1)).getBotToken("T002");
        then(methodsClient).should(times(2)).chatPostMessage(
                ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any());
    }

    @Test
    @DisplayName("UC-RS4: sendReportToUser — tokenResolver.getBotToken(workspaceId)로 토큰 조회")
    void sendReportToUser_usesTokenResolver() throws Exception {
        given(paymentRecordRepository.findByUserIdAndBilledAtBetween(any(), any(), any()))
                .willReturn(List.of());
        given(chartService.generatePieChartUrl(any())).willReturn(null);

        reportService.sendReportToUser(user, YearMonth.of(2024, 1));

        then(tokenResolver).should().getBotToken("T001");
        then(methodsClient).should(times(1)).chatPostMessage(
                ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any());
    }
}
