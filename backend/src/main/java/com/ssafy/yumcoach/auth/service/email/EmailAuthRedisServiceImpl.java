package com.ssafy.yumcoach.auth.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailAuthRedisServiceImpl implements EmailAuthRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.auth.email.ttl-seconds:180}")
    private long ttlSeconds;

    private static final String KEY_PREFIX = "email:verify:";

    @Override
    public void saveCode(String email, String code) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(KEY_PREFIX + email, code, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getCode(String email) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(KEY_PREFIX + email);
    }

    @Override
    public void deleteCode(String email) {
        redisTemplate.delete(KEY_PREFIX + email);
    }
}
