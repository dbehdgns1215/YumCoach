package com.ssafy.yumcoach.payments.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.yumcoach.payments.model.Subscription;

@Mapper
public interface SubscriptionMapper {
    void upsertSubscription(Subscription subscription);

    Subscription findByUserId(@Param("userId") Integer userId);
}
