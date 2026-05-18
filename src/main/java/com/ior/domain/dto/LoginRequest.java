package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求参数")
public class LoginRequest {

    @NotBlank(message = "标识符不能为空")
    @Schema(description = "账号/用户名/邮箱", example = "ior_user_001")
    private String identifier;

    @Schema(description = "密码（密码登录时必填）", example = "123456")
    private String password;

    @Schema(description = "验证码（验证码登录时必填）", example = "123456")
    private String code;

    @NotBlank(message = "登录方式不能为空")
    @Schema(description = "登录方式: PASSWORD-密码登录, CODE-验证码登录", example = "PASSWORD")
    private String loginType;
}
