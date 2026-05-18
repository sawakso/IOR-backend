package com.ior.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ior_verification_codes")
public class IorVerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收验证码的邮箱 */
    private String email;

    /** 验证码 */
    private String code;

    /** 验证码类型 */
    private String type;  // REGISTER, LOGIN, CHANGE_EMAIL, RESET_PASSWORD

    /** 是否已使用：0未使用, 1已使用 */
    private Integer used;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}