package com.ssafy.yumcoach.payments.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long id;
    private Integer userId;
    private String orderId;
    private String paymentKey;
    private String planType;
    private Integer amount;
    private String method;
    private String status;
    private LocalDateTime approvedAt;
    private String raw;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
