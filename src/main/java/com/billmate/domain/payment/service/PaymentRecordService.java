package com.billmate.domain.payment.service;

import com.billmate.domain.payment.entity.PaymentRecord;
import com.billmate.domain.payment.entity.PaymentStatus;
import com.billmate.domain.payment.repository.PaymentRecordRepository;
import com.billmate.domain.subscription.entity.Subscription;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;

    @Transactional
    public PaymentRecord record(Subscription subscription, BigDecimal amount, LocalDate billedAt) {
        return paymentRecordRepository.save(
                PaymentRecord.builder()
                        .subscription(subscription)
                        .amount(amount)
                        .billedAt(billedAt)
                        .status(PaymentStatus.PAID)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentRecord> getHistory(Subscription subscription) {
        return paymentRecordRepository.findBySubscriptionOrderByBilledAtDesc(subscription);
    }

    @Transactional(readOnly = true)
    public List<PaymentRecord> getHistoryByUser(User user, LocalDate from, LocalDate to) {
        return paymentRecordRepository.findByUserIdAndBilledAtBetween(user.getId(), from, to);
    }

    @Transactional(readOnly = true)
    public Map<SubscriptionCategory, BigDecimal> getCategoryTotals(User user, LocalDate from, LocalDate to) {
        List<PaymentRecord> records = paymentRecordRepository.findByUserIdAndBilledAtBetween(user.getId(), from, to);
        return records.stream()
                .collect(Collectors.groupingBy(
                        pr -> pr.getSubscription().getCategory(),
                        Collectors.reducing(BigDecimal.ZERO, PaymentRecord::getAmount, BigDecimal::add)
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSpendingPattern(User user) {
        LocalDate now = LocalDate.now();
        LocalDate sixMonthsAgo = now.minusMonths(6);
        List<PaymentRecord> records = paymentRecordRepository
                .findByUserIdAndBilledAtBetween(user.getId(), sixMonthsAgo, now);

        BigDecimal totalSpending = records.stream()
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<SubscriptionCategory, BigDecimal> categoryTotals = records.stream()
                .collect(Collectors.groupingBy(
                        pr -> pr.getSubscription().getCategory(),
                        Collectors.reducing(BigDecimal.ZERO, PaymentRecord::getAmount, BigDecimal::add)
                ));

        Map<SubscriptionCategory, BigDecimal> categoryMonthlyAvg = new LinkedHashMap<>();
        categoryTotals.forEach((cat, total) ->
                categoryMonthlyAvg.put(cat, total.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP))
        );

        Map<String, Object> pattern = new LinkedHashMap<>();
        pattern.put("totalSpending6Months", totalSpending);
        pattern.put("monthlyAverage", totalSpending.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP));
        pattern.put("annualEstimate", totalSpending.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(12)));
        pattern.put("categoryMonthlyAvg", categoryMonthlyAvg);
        return pattern;
    }
}
