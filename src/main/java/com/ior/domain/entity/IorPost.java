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
@TableName("ior_posts")
public class IorPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 作者ID
     */
    private Long userId;

    /**
     * 父帖子ID（0表示根帖子）
     */
    private Long parentId;

    /**
     * 当前标题
     */
    private String title;

    /**
     * 当前内容
     */
    private String content;

    /**
     * 媒体文件URL数组（JSON格式）
     */
    private String mediaUrls;

    /**
     * 可见性：PUBLIC-公开, PRIVATE-私密
     */
    private String visibility;

    /**
     * 状态：0-删除, 1-正常
     */
    private Integer status;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 总版本数量
     */
    private Integer versionCount;

    /**
     * 当前版本ID
     */
    private Long currentVersionId;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
