package com.ior.controller;

import com.ior.domain.vo.Result;
import com.ior.service.IorUsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "包含注册、登录、验证码等接口")
public class IorUsersController {

    @Resource
    private IorUsersService iorUsersService;

    @GetMapping("/sendcode")
    @Operation(summary = "发送邮箱验证码", description = "用于注册、登录或找回密码时的身份验证")
    public Result sendCode(
            @Parameter(description = "接收验证码的邮箱地址", required = true, example = "user@example.com") 
            @RequestParam String email,
            
            @Parameter(description = "验证码类型: register-注册, login-登录, reset_password-找回密码，change_email-重置邮箱", required = true, example = "register")
            @RequestParam String type) {
        return iorUsersService.sendCode(email, type);
    }


}
