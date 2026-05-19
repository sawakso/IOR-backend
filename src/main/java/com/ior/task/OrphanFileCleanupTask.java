package com.ior.task;

import com.ior.service.R2StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 孤儿文件清理任务
 * 定期清理 R2 中未被引用的临时文件
 */
@Slf4j
@Component
public class OrphanFileCleanupTask {

    private final R2StorageService r2StorageService;

    public OrphanFileCleanupTask(R2StorageService r2StorageService) {
        this.r2StorageService = r2StorageService;
    }

    /**
     * 每天凌晨 3 点执行清理
     * cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanFiles() {
        log.info("开始清理孤儿文件...");
        
        try {
            // 清理 posts/temp 目录下超过 24 小时的文件
            int deletedCount = r2StorageService.deleteOldTempFiles(24);
            
            log.info("孤儿文件清理完成，共删除 {} 个文件", deletedCount);
        } catch (Exception e) {
            log.error("孤儿文件清理失败", e);
        }
    }

    /**
     * 每小时执行一次（可选，用于更频繁的清理）
     */
    // @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOrphanFilesHourly() {
        log.info("开始 hourly 清理孤儿文件...");
        
        try {
            int deletedCount = r2StorageService.deleteOldTempFiles(1);
            log.info("Hourly 清理完成，共删除 {} 个文件", deletedCount);
        } catch (Exception e) {
            log.error("Hourly 清理失败", e);
        }
    }
}
