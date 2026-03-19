package com.billmate.domain.payment.repository;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.repository.SubscriptionRepository;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.repository.UserRepository;
import com.billmate.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PaymentRecordRepositoryTest {

    @Autowired
    PaymentRecordRepository paymentRecordRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    TestEntityManager em;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("U001"));
        subscription = subscriptionRepository.save(
                TestFixtures.subscription(user, "Netflix", SubscriptionCategory.OTT, 25));
    }

    @Test
    @DisplayName("UC-P1: 날짜 범위 안의 결제 이력만 findByUserIdAndBilledAtBetween으로 반환")
    void findByUserIdAndBilledAtBetween_returnsInRange() {
        paymentRecordRepository.save(TestFixtures.paymentRecord(subscription, new BigDecimal("9900"),
                LocalDate.of(2024, 1, 25)));
        paymentRecordRepository.save(TestFixtures.paymentRecord(subscription, new BigDecimal("9900"),
                LocalDate.of(2024, 2, 25)));
        paymentRecordRepository.save(TestFixtures.paymentRecord(subscription, new BigDecimal("9900"),
                LocalDate.of(2024, 4, 25)));
        em.flush();
        em.clear();

        List<PaymentRecord> result = paymentRecordRepository.findByUserIdAndBilledAtBetween(
                user.getId(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 31));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("UC-P2: findBySubscriptionOrderByBilledAtDesc — 최신 결제일 순 정렬 검증")
    void findBySubscriptionOrderByBilledAtDesc() {
        paymentRecordRepository.save(TestFixtures.paymentRecord(subscription, new BigDecimal("9900"),
                LocalDate.of(2024, 1, 25)));
        paymentRecordRepository.save(TestFixtures.paymentRecord(subscription, new BigDecimal("9900"),
                LocalDate.of(2024, 3, 25)));
        paymentRecordRepository.save(TestFixtures.paymentRecord(subscription, new BigDecimal("9900"),
                LocalDate.of(2024, 2, 25)));
        em.flush();
        em.clear();

        List<PaymentRecord> result = paymentRecordRepository.findBySubscriptionOrderByBilledAtDesc(subscription);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getBilledAt()).isEqualTo(LocalDate.of(2024, 3, 25));
        assertThat(result.get(2).getBilledAt()).isEqualTo(LocalDate.of(2024, 1, 25));
    }
}
