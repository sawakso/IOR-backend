package com.ior.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应")
public class UserInfoVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户账号", example = "ior_user_001")
    private String account;

    @Schema(description = "用户名", example = "IOR大师")
    private String username;

    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;

    @Schema(description = "昵称", example = "IOR大神")
    private String nickname;

    @Schema(description = "头像URL", example = "https://r2picals.sawakso.com/avatars/xxx.jpg")
    private String avatarUrl;

    @Schema(description = "角色：USER普通用户, VIP会员, ADMIN管理员", example = "USER")
    private String role;

    @Schema(description = "状态：0-禁用, 1-正常, 2-待注销", example = "1")
    private Integer status;

    @Schema(description = "最后登录时间", example = "2026-05-19T10:30:00")
    private LocalDateTime lastLoginAt;

    @Schema(description = "创建时间", example = "2026-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2026-05-19T10:30:00")
    private LocalDateTime updatedAt;
}
