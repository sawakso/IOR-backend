package com.ior.strategy;

import com.ior.strategy.impl.DbCodeStrategy;
import com.ior.strategy.impl.RedisCodeStrategy;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 验证码策略上下文：根据配置自动切换存储方式
 */
@Component
public class VerificationCodeContext {

    @Value("${verify.code.storage:redis}") // 默认使用 redis
    private String storageType;

    @Resource(name = "redisCodeStrategy")
    private VerificationCodeStrategy redisStrategy;

    @Resource(name = "dbCodeStrategy")
    private VerificationCodeStrategy dbStrategy;

    /**
     * 获取当前启用的策略
     */
    private VerificationCodeStrategy getStrategy() {
        if ("db".equalsIgnoreCase(storageType)) {
            return dbStrategy;
        }
        return redisStrategy;
    }

    public void save(String email, String code, String type, long expireSeconds) {
        getStrategy().save(email, code, type, expireSeconds);
    }

    public String get(String email, String type) {
        return getStrategy().get(email, type);
    }

    public void remove(String email, String type) {
        getStrategy().remove(email, type);
    }
}
