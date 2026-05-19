package com.ior.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存助手类
 * 实现 Cache-Aside（旁路缓存）模式：
 * 1. 读操作：先查缓存，命中则返回；未命中则查数据库，查到后写入缓存
 * 2. 写操作：先更新数据库，再删除缓存（保证一致性）
 */
@Slf4j
@Component
public class RedisCacheHelper {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 通用缓存操作 ====================

    /**
     * 获取缓存（自动反序列化为指定类型）
     * @param key 缓存键
     * @param clazz 目标类型
     * @return 缓存对象，不存在则返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Redis 缓存反序列化失败, key: {}", key, e);
            return null;
        }
    }

    /**
     * 设置缓存（自动序列化）
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间（分钟）
     */
    public void set(String key, Object value, long ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, ttl, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Redis 缓存序列化失败, key: {}", key, e);
        }
    }

    /**
     * 删除缓存
     * @param key 缓存键
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
        log.debug("删除缓存: {}", key);
    }

    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * @return true-存在，false-不存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ==================== Cache-Aside 模式封装 ====================

    /**
     * 查询缓存，未命中时执行 loader 函数加载数据并写入缓存
     * 
     * 使用示例：
     * <pre>
     * User user = cacheHelper.getOrLoad(
     *     RedisConstants.userInfoKey(userId),
     *     RedisConstants.USER_INFO_TTL,
     *     () -> userMapper.selectById(userId)
     * );
     * </pre>
     *
     * @param key 缓存键
     * @param ttl 过期时间（分钟）
     * @param loader 数据加载函数（通常是数据库查询）
     * @return 缓存或数据库中的数据
     */
    public <T> T getOrLoad(String key, long ttl, DataLoader<T> loader) {
        // 1. 尝试从缓存获取
        T data = get(key, (Class<T>) Object.class);
        if (data != null) {
            log.debug("缓存命中: {}", key);
            return data;
        }

        // 2. 缓存未命中，从数据库加载
        log.debug("缓存未命中，查询数据库: {}", key);
        data = loader.load();

        // 3. 如果数据存在，写入缓存
        if (data != null) {
            set(key, data, ttl);
            log.debug("写入缓存: {}, TTL: {}分钟", key, ttl);
        }

        return data;
    }

    /**
     * 更新数据后删除缓存（保证一致性）
     * 
     * 使用示例：
     * <pre>
     * // 1. 更新数据库
     * userMapper.updateById(user);
     * // 2. 删除缓存
     * cacheHelper.invalidate(RedisConstants.userInfoKey(user.getId()));
     * </pre>
     *
     * @param key 缓存键
     */
    public void invalidate(String key) {
        delete(key);
    }

    /**
     * 批量删除缓存（支持通配符）
     * @param pattern 匹配模式，如 "ior:post:detail:*"
     */
    public void invalidatePattern(String pattern) {
        stringRedisTemplate.keys(pattern).forEach(key -> {
            stringRedisTemplate.delete(key);
            log.debug("删除缓存（模式匹配）: {}", key);
        });
    }

    // ==================== 函数式接口 ====================

    /**
     * 数据加载器函数式接口
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        T load();
    }
}
