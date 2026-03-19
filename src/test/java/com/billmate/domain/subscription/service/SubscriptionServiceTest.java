package com.billmate.domain.subscription.service;

import com.billmate.domain.notification.service.NotificationService;
import com.billmate.domain.subscription.dto.SubscriptionCreateRequest;
import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.repository.SubscriptionRepository;
import com.billmate.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    SubscriptionService subscriptionService;

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
    @DisplayName("UC-SS1: create() 호출 시 notificationService.createDefaults()가 반드시 1회 호출됨")
    void create_callsCreateDefaults() {
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(subscription);

        SubscriptionCreateRequest request = SubscriptionCreateRequest.builder()
                .serviceName("Netflix")
                .category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900"))
                .billingDay(25)
                .billingCycle(BillingCycle.MONTHLY)
                .build();

        subscriptionService.create(user, request);

        then(notificationService).should(times(1)).createDefaults(eq(user), any(Subscription.class));
    }

    @Test
    @DisplayName("UC-SS2: delete() — 다른 유저 소유 구독 → IllegalArgumentException")
    void delete_notOwner_throwsException() {
        given(subscriptionRepository.findByIdAndUserAndIsActiveTrue(99L, user))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.delete(user, 99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UC-SS3: listByUserAndCategory() — repository에 category 파라미터 전달 검증")
    void listByUserAndCategory_passesCategory() {
        given(subscriptionRepository.findByUserAndCategoryAndIsActiveTrue(user, SubscriptionCategory.OTT))
                .willReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.listByUserAndCategory(user, SubscriptionCategory.OTT);

        then(subscriptionRepository).should().findByUserAndCategoryAndIsActiveTrue(user, SubscriptionCategory.OTT);
        assertThat(result).hasSize(1);
    }
}
