package com.billmate.domain.shared.service;

import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.shared.repository.SharedAccountRepository;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountServiceTest {

    @Mock
    SharedAccountRepository sharedAccountRepository;

    @InjectMocks
    SharedAccountService sharedAccountService;

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
    @DisplayName("UC-SHA1: addMember() — 이미 존재하는 멤버 추가 시 IllegalStateException")
    void addMember_alreadyExists_throwsException() {
        given(sharedAccountRepository.existsBySubscriptionAndMemberSlackUserId(subscription, "U002"))
                .willReturn(true);

        assertThatThrownBy(() -> sharedAccountService.addMember(subscription, "U002", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("U002");
    }

    @Test
    @DisplayName("UC-SHA2: removeMember() — 존재하는 멤버 제거 시 delete 호출")
    void removeMember_existingMember_deletesCalled() {
        SharedAccount sa = SharedAccount.builder()
                .subscription(subscription)
                .memberSlackUserId("U002")
                .build();
        given(sharedAccountRepository.findBySubscriptionAndMemberSlackUserId(subscription, "U002"))
                .willReturn(Optional.of(sa));

        sharedAccountService.removeMember(subscription, "U002");

        then(sharedAccountRepository).should().delete(sa);
    }
}
