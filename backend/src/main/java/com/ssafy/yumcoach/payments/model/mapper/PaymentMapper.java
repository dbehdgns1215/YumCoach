package com.ssafy.yumcoach.payments.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.yumcoach.payments.model.Payment;

@Mapper
public interface PaymentMapper {
    void insertPayment(Payment payment);

    Payment findByOrderId(@Param("orderId") String orderId);

    void updatePaymentStatus(@Param("orderId") String orderId, @Param("status") String status);
}
