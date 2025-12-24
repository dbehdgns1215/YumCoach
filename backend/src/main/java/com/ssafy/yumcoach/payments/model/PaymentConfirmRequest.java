package com.ssafy.yumcoach.payments.model;

import lombok.Data;

@Data
public class PaymentConfirmRequest {
    private String paymentKey;
    private String orderId;
    private Integer amount;
    private Integer userId; // optional: 결제자 ID
    private String planType; // optional: 'monthly' | 'yearly'
}
