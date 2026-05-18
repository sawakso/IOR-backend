package com.ior.strategy;

import com.ior.domain.entity.IorUsers;

/**
 * 用户注销处理策略
 */
public interface UserDeletionStrategy {
    
    /**
     * 执行注销逻辑
     * @param user 待注销的用户
     */
    void execute(IorUsers user);

    /**
     * 该策略支持的角色
     * @return 角色名称
     */
    String supportRole();
}
