package com.billmate.domain.subscription.repository;

import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SubscriptionRepositoryTest {

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("U001"));
    }

    @Test
    @DisplayName("UC-S1: isActive=true인 구독만 findByUserAndIsActiveTrue로 반환된다")
    void findActiveSubscriptions() {
        subscriptionRepository.save(TestFixtures.subscription(user, "Netflix", SubscriptionCategory.OTT, 25));
        subscriptionRepository.save(TestFixtures.subscription(user, "Spotify", SubscriptionCategory.MUSIC_STREAMING, 15));
        em.flush();
        em.clear();

        List<Subscription> result = subscriptionRepository.findByUserAndIsActiveTrue(user);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("UC-S2: deactivate() 후 조회 시 결과에 포함되지 않는다")
    void deactivatedSubscriptionNotReturned() {
        Subscription sub = subscriptionRepository.save(
                TestFixtures.subscription(user, "Netflix", SubscriptionCategory.OTT, 25));
        sub.deactivate();
        subscriptionRepository.save(sub);
        em.flush();
        em.clear();

        List<Subscription> result = subscriptionRepository.findByUserAndIsActiveTrue(user);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UC-S3: 카테고리 필터링 — OTT만 조회 시 다른 카테고리 제외")
    void filterByCategory() {
        subscriptionRepository.save(TestFixtures.subscription(user, "Netflix", SubscriptionCategory.OTT, 25));
        subscriptionRepository.save(TestFixtures.subscription(user, "Spotify", SubscriptionCategory.MUSIC_STREAMING, 15));
        em.flush();
        em.clear();

        List<Subscription> result = subscriptionRepository.findByUserAndCategoryAndIsActiveTrue(
                user, SubscriptionCategory.OTT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServiceName()).isEqualTo("Netflix");
    }

    @Test
    @DisplayName("UC-S4: 다른 유저의 구독은 findByIdAndUserAndIsActiveTrue에서 empty 반환")
    void otherUserSubscriptionReturnsEmpty() {
        User otherUser = userRepository.save(TestFixtures.user("U002"));
        Subscription sub = subscriptionRepository.save(
                TestFixtures.subscription(otherUser, "Netflix", SubscriptionCategory.OTT, 25));
        em.flush();
        em.clear();

        Optional<Subscription> result = subscriptionRepository.findByIdAndUserAndIsActiveTrue(sub.getId(), user);

        assertThat(result).isEmpty();
    }
}
