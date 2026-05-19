package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "重置密码请求参数")
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "用户邮箱", example = "user@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", example = "newpassword123")
    private String newPassword;
}
