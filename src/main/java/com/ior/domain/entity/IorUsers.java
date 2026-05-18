package com.ior.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ior_users")
public class IorUsers implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户账号（唯一，不可修改） */
    private String account;

    /** 用户名（唯一，不可修改） */
    private String username;

    /** 邮箱地址 */
    private String email;

    /** 密码哈希值（BCrypt加密） */
    @TableField(select = false) // 查询时默认不返回密码
    private String passwordHash;

    /** 昵称（可修改） */
    private String nickname;

    /** 头像URL（存储于R2/S3） */
    private String avatarUrl;

    /** 角色：USER普通用户, VIP会员, ADMIN管理员 */
    private String role;

    /** 状态：0-禁用, 1-正常, 2-待注销（冷静期） */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 最后登录IP地址 */
    private String lastLoginIp;

    /** 注销时间（逻辑删除标记，1970-01-01表示未删除） */
    @TableLogic(value = "1970-01-01 00:00:01", delval = "now()")
    private LocalDateTime deletedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
