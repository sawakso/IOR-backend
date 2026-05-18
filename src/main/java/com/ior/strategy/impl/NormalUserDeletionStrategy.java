package com.ior.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ior.domain.entity.IorUsers;
import com.ior.mapper.IorUsersMapper;
import com.ior.strategy.UserDeletionStrategy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 普通用户注销策略：执行逻辑删除
 */
@Component
public class NormalUserDeletionStrategy implements UserDeletionStrategy {

    @Resource
    private IorUsersMapper usersMapper;

    @Override
    public void execute(IorUsers user) {
        // 执行逻辑删除：将 deleted_at 设置为当前时间
        // 注意：由于我们在 Entity 中配置了 @TableLogic，直接调用 update 即可
        LambdaUpdateWrapper<IorUsers> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IorUsers::getId, user.getId())
               .set(IorUsers::getDeletedAt, LocalDateTime.now());
        usersMapper.update(null, wrapper);
        
        System.out.println("用户 " + user.getAccount() + " 已被逻辑删除");
    }

    @Override
    public String supportRole() {
        return "USER";
    }
}
