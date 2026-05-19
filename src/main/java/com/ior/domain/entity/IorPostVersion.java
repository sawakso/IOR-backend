package com.ior.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ior_post_versions")
public class IorPostVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的帖子ID
     */
    private Long postId;

    /**
     * 版本号（从1开始递增）
     */
    private Integer versionNumber;

    /**
     * 标题快照
     */
    private String title;

    /**
     * 内容快照
     */
    private String content;

    /**
     * 媒体文件URL快照（JSON格式）
     */
    private String mediaUrls;

    /**
     * 修改说明
     */
    private String changeSummary;

    /**
     * 版本创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 修改者ID
     */
    private Long createdBy;
}
