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

    @Value("${app.auth.email.verified-ttl-seconds:600}")
    private long verifiedTtlSeconds;

    private static final String KEY_PREFIX = "email:verify:";
    private static final String VERIFIED_PREFIX = "email:verified:";

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

    @Override
    public void saveVerified(String email) {
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "1", verifiedTtlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isVerified(String email) {
        String v = redisTemplate.opsForValue().get(VERIFIED_PREFIX + email);
        return v != null;
    }
}
