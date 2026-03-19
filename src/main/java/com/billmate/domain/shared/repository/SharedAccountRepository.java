package com.billmate.domain.shared.repository;

import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedAccountRepository extends JpaRepository<SharedAccount, Long> {

    List<SharedAccount> findBySubscription(Subscription subscription);

    Optional<SharedAccount> findBySubscriptionAndMemberSlackUserId(Subscription subscription, String memberSlackUserId);

    boolean existsBySubscriptionAndMemberSlackUserId(Subscription subscription, String memberSlackUserId);
}
