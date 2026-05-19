package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户注销申请请求参数")
public class DeletionRequest {

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String code;

    @Schema(description = "注销原因（可选）", example = "不再使用该平台")
    private String reason;
}
