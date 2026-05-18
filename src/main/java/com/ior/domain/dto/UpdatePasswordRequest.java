package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class UpdatePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "当前密码", example = "123456")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", example = "654321")
    private String newPassword;
}
