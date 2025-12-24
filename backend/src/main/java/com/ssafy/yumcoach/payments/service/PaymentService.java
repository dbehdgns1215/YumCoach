package com.ssafy.yumcoach.payments.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.yumcoach.payments.model.Payment;
import com.ssafy.yumcoach.payments.model.Subscription;
import com.ssafy.yumcoach.payments.model.mapper.PaymentMapper;
import com.ssafy.yumcoach.payments.model.mapper.SubscriptionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ObjectMapper objectMapper;

    /** 결제 성공 시 DB 저장 및 구독 갱신 */
    public void recordPaymentAndSubscription(Map<String, Object> paymentData, Integer userId, String planType) {
        Payment payment = buildPayment(paymentData, userId, planType);
        log.debug("Persisting payment: orderId={}, userId={}, amount={}, status={}, method={}, approvedAt={}",
                payment.getOrderId(), payment.getUserId(), payment.getAmount(), payment.getStatus(),
                payment.getMethod(), payment.getApprovedAt());
        try {
            paymentMapper.insertPayment(payment);
        } catch (Exception e) {
            log.error("Failed to insert payment orderId={}: {}", payment.getOrderId(), e.getMessage(), e);
            throw e;
        }

        if (userId != null) {
            String resolvedPlan = resolvePlanType(planType, payment.getOrderId(),
                    (String) paymentData.get("orderName"));
            Subscription subscription = buildSubscription(userId, resolvedPlan);
            log.debug("Upserting subscription: userId={}, planType={}, start={}, end={}",
                    subscription.getUserId(), subscription.getPlanType(), subscription.getStartDate(),
                    subscription.getEndDate());
            try {
                subscriptionMapper.upsertSubscription(subscription);
            } catch (Exception e) {
                log.error("Failed to upsert subscription for userId={}: {}", userId, e.getMessage(), e);
                throw e;
            }
        } else {
            log.warn("Payment saved without userId; subscription not updated. orderId={}", payment.getOrderId());
        }
    }

    private Payment buildPayment(Map<String, Object> paymentData, Integer userId, String planType) {
        String orderId = (String) paymentData.getOrDefault("orderId", "");
        String paymentKey = (String) paymentData.getOrDefault("paymentKey", "");
        String resolvedPlan = resolvePlanType(planType, orderId, (String) paymentData.get("orderName"));
        Integer amount = extractInt(paymentData.get("totalAmount"), paymentData.get("amount"));
        String method = (String) paymentData.get("method");
        String status = (String) paymentData.getOrDefault("status", "DONE");
        LocalDateTime approvedAt = parseDate((String) paymentData.get("approvedAt"));

        String raw = serialize(paymentData);

        return Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentKey(paymentKey)
                .planType(resolvedPlan)
                .amount(amount)
                .method(method)
                .status(status)
                .approvedAt(approvedAt)
                .raw(raw)
                .build();
    }

    private Subscription buildSubscription(Integer userId, String planType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = "yearly".equalsIgnoreCase(planType) ? now.plusYears(1) : now.plusMonths(1);

        return Subscription.builder()
                .userId(userId)
                .planType(planType)
                .status("ACTIVE")
                .startDate(now)
                .endDate(end)
                .build();
    }

    private String resolvePlanType(String planType, String orderId, String orderName) {
        if (planType != null && !planType.isBlank())
            return planType;
        if (orderId != null && orderId.toLowerCase().contains("year"))
            return "yearly";
        if (orderName != null && orderName.contains("연간"))
            return "yearly";
        if (orderName != null && orderName.toLowerCase().contains("year"))
            return "yearly";
        return "monthly";
    }

    private Integer extractInt(Object... values) {
        for (Object v : values) {
            if (v == null)
                continue;
            if (v instanceof Number)
                return ((Number) v).intValue();
            if (v instanceof String) {
                try {
                    return Integer.parseInt((String) v);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private LocalDateTime parseDate(String isoString) {
        if (isoString == null || isoString.isBlank())
            return null;
        try {
            return OffsetDateTime.parse(isoString).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(isoString);
            } catch (Exception ignored) {
                log.warn("Failed to parse approvedAt: {}", isoString);
                return null;
            }
        }
    }

    private String serialize(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payment raw data", e);
            return null;
        }
    }
}
