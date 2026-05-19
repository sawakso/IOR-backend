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
@TableName("ior_post_media")
public class IorPostMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的帖子ID
     */
    private Long postId;

    /**
     * 关联的版本ID
     */
    private Long versionId;

    /**
     * 文件URL（R2存储地址）
     */
    private String fileUrl;

    /**
     * 文件类型：IMAGE-图片, VIDEO-视频
     */
    private String fileType;

    /**
     * 文件格式（jpg, png, mp4等）
     */
    private String fileFormat;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 图片宽度（像素）
     */
    private Integer width;

    /**
     * 图片高度（像素）
     */
    private Integer height;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * 排序权重（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 状态：0-删除, 1-正常
     */
    private Integer status;

    /**
     * 上传时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
