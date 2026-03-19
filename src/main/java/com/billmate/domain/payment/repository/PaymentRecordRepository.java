package com.billmate.domain.payment.repository;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.entity.PaymentStatus;
import com.billmate.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    List<PaymentRecord> findBySubscriptionOrderByBilledAtDesc(Subscription subscription);

    @Query("""
        SELECT pr FROM PaymentRecord pr
        JOIN FETCH pr.subscription s
        WHERE s.user.id = :userId
          AND pr.billedAt BETWEEN :from AND :to
        ORDER BY pr.billedAt DESC
        """)
    List<PaymentRecord> findByUserIdAndBilledAtBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT pr FROM PaymentRecord pr
        JOIN FETCH pr.subscription s
        WHERE pr.billedAt BETWEEN :from AND :to
          AND pr.status = :status
        """)
    List<PaymentRecord> findByBilledAtBetweenAndStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") PaymentStatus status);
}
