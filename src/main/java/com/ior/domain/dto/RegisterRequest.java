package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户注册请求参数")
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户账号（唯一）", example = "ior_user_001")
    private String account;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名（唯一）", example = "IOR大师")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "123456")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String code;
}
