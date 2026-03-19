package com.billmate.domain.shared.repository;

import com.billmate.domain.shared.entity.SharedAccount;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SharedAccountRepositoryTest {

    @Autowired
    SharedAccountRepository sharedAccountRepository;

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
    @DisplayName("UC-SA1: 동일 (subscription, memberSlackUserId) 조합 중복 저장 → unique 예외")
    void duplicateMemberThrows() {
        sharedAccountRepository.save(TestFixtures.sharedAccount(subscription, "U002"));
        em.flush();

        assertThatThrownBy(() -> {
            sharedAccountRepository.saveAndFlush(TestFixtures.sharedAccount(subscription, "U002"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UC-SA2: findBySubscription — 해당 구독의 모든 멤버 조회")
    void findBySubscription() {
        sharedAccountRepository.save(TestFixtures.sharedAccount(subscription, "U002"));
        sharedAccountRepository.save(TestFixtures.sharedAccount(subscription, "U003"));
        em.flush();
        em.clear();

        List<SharedAccount> result = sharedAccountRepository.findBySubscription(subscription);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("UC-SA3: existsBySubscriptionAndMemberSlackUserId — 존재/미존재 각각 검증")
    void existsBySubscriptionAndMemberSlackUserId() {
        sharedAccountRepository.save(TestFixtures.sharedAccount(subscription, "U002"));
        em.flush();
        em.clear();

        assertThat(sharedAccountRepository.existsBySubscriptionAndMemberSlackUserId(subscription, "U002")).isTrue();
        assertThat(sharedAccountRepository.existsBySubscriptionAndMemberSlackUserId(subscription, "U999")).isFalse();
    }
}
