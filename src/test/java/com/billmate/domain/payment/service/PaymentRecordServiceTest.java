package com.billmate.domain.payment.service;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.entity.PaymentStatus;
import com.billmate.domain.payment.repository.PaymentRecordRepository;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRecordServiceTest {

    @Mock
    PaymentRecordRepository paymentRecordRepository;

    @InjectMocks
    PaymentRecordService paymentRecordService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder().slackUserId("U001").slackWorkspaceId("W001").displayName("Alice").build();
        subscription = Subscription.builder()
                .user(user)
                .serviceName("Netflix")
                .category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900"))
                .billingDay(25)
                .billingCycle(BillingCycle.MONTHLY)
                .isActive(true)
                .startedAt(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("UC-PR1: record() — status=PAID로 저장")
    void record_saveWithStatusPaid() {
        PaymentRecord saved = PaymentRecord.builder()
                .subscription(subscription)
                .amount(new BigDecimal("15900"))
                .billedAt(LocalDate.of(2024, 1, 25))
                .status(PaymentStatus.PAID)
                .build();
        given(paymentRecordRepository.save(any(PaymentRecord.class))).willReturn(saved);

        PaymentRecord result = paymentRecordService.record(subscription, new BigDecimal("15900"),
                LocalDate.of(2024, 1, 25));

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        then(paymentRecordRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("UC-PR2: getSpendingPattern() — totalSpending, monthlyAverage, annualEstimate 계산 검증")
    void getSpendingPattern_calculatesCorrectly() {
        // 6개월간 총 60000원 (매월 10000원)
        Subscription sub1 = Subscription.builder()
                .user(user).serviceName("S1").category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("10000")).billingDay(1)
                .billingCycle(BillingCycle.MONTHLY).isActive(true).startedAt(LocalDate.now()).build();

        List<PaymentRecord> records = List.of(
                PaymentRecord.builder().subscription(sub1).amount(new BigDecimal("10000"))
                        .billedAt(LocalDate.now().minusMonths(5)).status(PaymentStatus.PAID).build(),
                PaymentRecord.builder().subscription(sub1).amount(new BigDecimal("10000"))
                        .billedAt(LocalDate.now().minusMonths(4)).status(PaymentStatus.PAID).build(),
                PaymentRecord.builder().subscription(sub1).amount(new BigDecimal("10000"))
                        .billedAt(LocalDate.now().minusMonths(3)).status(PaymentStatus.PAID).build(),
                PaymentRecord.builder().subscription(sub1).amount(new BigDecimal("10000"))
                        .billedAt(LocalDate.now().minusMonths(2)).status(PaymentStatus.PAID).build(),
                PaymentRecord.builder().subscription(sub1).amount(new BigDecimal("10000"))
                        .billedAt(LocalDate.now().minusMonths(1)).status(PaymentStatus.PAID).build(),
                PaymentRecord.builder().subscription(sub1).amount(new BigDecimal("10000"))
                        .billedAt(LocalDate.now()).status(PaymentStatus.PAID).build()
        );
        given(paymentRecordRepository.findByUserIdAndBilledAtBetween(any(), any(), any()))
                .willReturn(records);

        Map<String, Object> pattern = paymentRecordService.getSpendingPattern(user);

        assertThat((BigDecimal) pattern.get("totalSpending6Months"))
                .isEqualByComparingTo(new BigDecimal("60000"));
        assertThat((BigDecimal) pattern.get("monthlyAverage"))
                .isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat((BigDecimal) pattern.get("annualEstimate"))
                .isEqualByComparingTo(new BigDecimal("120000.00"));
    }
}
