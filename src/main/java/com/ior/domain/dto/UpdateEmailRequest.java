package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改邮箱请求")
public class UpdateEmailRequest {

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "新的邮箱地址", example = "new_email@example.com")
    private String newEmail;

    @NotBlank(message = "旧邮箱验证码不能为空")
    @Schema(description = "发送到旧邮箱的验证码", example = "123456")
    private String oldEmailCode;

    @NotBlank(message = "新邮箱验证码不能为空")
    @Schema(description = "发送到新邮箱的验证码", example = "654321")
    private String newEmailCode;
}
