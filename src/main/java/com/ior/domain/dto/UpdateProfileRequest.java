package com.ior.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新个人资料请求")
public class UpdateProfileRequest {

    @Schema(description = "昵称", example = "IOR大神")
    private String nickname;

    @Schema(description = "头像URL", example = "https://r2.example.com/avatar.jpg")
    private String avatarUrl;
}
