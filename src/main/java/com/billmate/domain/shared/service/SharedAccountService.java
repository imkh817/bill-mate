package com.billmate.domain.shared.service;

import com.billmate.domain.shared.entity.SharedAccount;
import com.billmate.domain.shared.entity.SharedRole;
import com.billmate.domain.shared.repository.SharedAccountRepository;
import com.billmate.domain.subscription.entity.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedAccountService {

    private final SharedAccountRepository sharedAccountRepository;

    @Transactional
    public SharedAccount addMember(Subscription subscription, String memberSlackUserId, BigDecimal splitAmount) {
        if (sharedAccountRepository.existsBySubscriptionAndMemberSlackUserId(subscription, memberSlackUserId)) {
            throw new IllegalStateException("Member already added: " + memberSlackUserId);
        }
        return sharedAccountRepository.save(
                SharedAccount.builder()
                        .subscription(subscription)
                        .memberSlackUserId(memberSlackUserId)
                        .role(SharedRole.MEMBER)
                        .splitAmount(splitAmount)
                        .build()
        );
    }

    @Transactional
    public void removeMember(Subscription subscription, String memberSlackUserId) {
        sharedAccountRepository.findBySubscriptionAndMemberSlackUserId(subscription, memberSlackUserId)
                .ifPresent(sharedAccountRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<SharedAccount> getMembers(Subscription subscription) {
        return sharedAccountRepository.findBySubscription(subscription);
    }
}
