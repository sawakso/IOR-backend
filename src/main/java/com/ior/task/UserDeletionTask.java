package com.ior.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ior.domain.entity.IorUserDeletionRequest;
import com.ior.domain.entity.IorUsers;
import com.ior.mapper.IorUserDeletionRequestMapper;
import com.ior.mapper.IorUsersMapper;
import com.ior.strategy.DeletionStrategyContext;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class UserDeletionTask {

    @Resource
    private IorUserDeletionRequestMapper deletionRequestMapper;

    @Resource
    private IorUsersMapper usersMapper;

    @Resource
    private DeletionStrategyContext deletionStrategyContext;

    /**
     * 每天凌晨 2 点执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processExpiredDeletions() {
        System.out.println("开始执行用户注销定时任务...");

        // 1. 查找所有已过期的注销申请 (completed_at < 当前时间 且 状态为 PENDING)
        LambdaQueryWrapper<IorUserDeletionRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUserDeletionRequest::getStatus, "PENDING")
               .lt(IorUserDeletionRequest::getCompletedAt, LocalDateTime.now());

        List<IorUserDeletionRequest> expiredRequests = deletionRequestMapper.selectList(wrapper);

        for (IorUserDeletionRequest request : expiredRequests) {
            try {
                // 2. 获取用户信息
                IorUsers user = usersMapper.selectById(request.getUserId());
                if (user != null) {
                    // 3. 根据角色选择策略并执行注销
                    deletionStrategyContext.getStrategy(user.getRole()).execute(user);
                }

                // 4. 更新注销申请状态为 COMPLETED
                request.setStatus("COMPLETED");
                deletionRequestMapper.updateById(request);

            } catch (Exception e) {
                System.err.println("处理用户注销失败，ID: " + request.getUserId());
                e.printStackTrace();
            }
        }
        
        System.out.println("用户注销定时任务执行完毕，共处理 " + expiredRequests.size() + " 条记录。");
    }
}
