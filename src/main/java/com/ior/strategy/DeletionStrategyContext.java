package com.ior.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 注销策略上下文：根据用户角色选择对应的注销处理方式
 */
@Component
public class DeletionStrategyContext {

    private final Map<String, UserDeletionStrategy> strategyMap;

    public DeletionStrategyContext(List<UserDeletionStrategy> strategies) {
        // 将所有策略注入到一个 Map 中，Key 为角色名
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(UserDeletionStrategy::supportRole, Function.identity()));
    }

    /**
     * 获取对应角色的注销策略
     */
    public UserDeletionStrategy getStrategy(String role) {
        // 如果没有特定角色的策略，默认使用 USER 策略
        return strategyMap.getOrDefault(role, strategyMap.get("USER"));
    }
}
