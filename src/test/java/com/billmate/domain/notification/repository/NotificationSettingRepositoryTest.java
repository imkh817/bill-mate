package com.billmate.domain.notification.repository;

import com.billmate.domain.notification.entity.NotificationSetting;
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

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class NotificationSettingRepositoryTest {

    @Autowired
    NotificationSettingRepository notificationSettingRepository;

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
    @DisplayName("UC-N1: billingDay=25, daysBeforeBilling=3이면 해당 구독의 D-3 알림 반환")
    void findDueNotifications_returnsMatchingNotification() {
        notificationSettingRepository.save(
                TestFixtures.notificationSetting(user, subscription, 3, true));
        em.flush();
        em.clear();

        List<NotificationSetting> result = notificationSettingRepository.findDueNotifications(25, 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDaysBeforeBilling()).isEqualTo(3);
    }

    @Test
    @DisplayName("UC-N2: isEnabled=false인 알림은 findDueNotifications에서 제외")
    void findDueNotifications_excludesDisabled() {
        notificationSettingRepository.save(
                TestFixtures.notificationSetting(user, subscription, 3, false));
        em.flush();
        em.clear();

        List<NotificationSetting> result = notificationSettingRepository.findDueNotifications(25, 3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UC-N3: findByUserAndSubscription — user+subscription 조합으로 조회")
    void findByUserAndSubscription() {
        notificationSettingRepository.save(TestFixtures.notificationSetting(user, subscription, 3, true));
        notificationSettingRepository.save(TestFixtures.notificationSetting(user, subscription, 1, true));
        em.flush();
        em.clear();

        List<NotificationSetting> result = notificationSettingRepository.findByUserAndSubscription(user, subscription);

        assertThat(result).hasSize(2);
    }
}
