package com.ior.strategy.impl;

import com.ior.strategy.VerificationCodeStrategy;
import com.ior.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("redisCodeStrategy")
@RequiredArgsConstructor
public class RedisCodeStrategy implements VerificationCodeStrategy {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String email, String code, String type, long expireSeconds) {
        String key = RedisConstants.codeKey(type, email);
        stringRedisTemplate.opsForValue()
                .set(key, code, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String email, String type) {
        if (email == null) {
            return null;
        }
        String key = RedisConstants.codeKey(type, email);
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void remove(String email, String type) {
        if (email != null) {
            String key = RedisConstants.codeKey(type, email);
            stringRedisTemplate.delete(key);
        }
    }
}