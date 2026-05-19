package com.ior.controller;

import com.ior.domain.dto.LoginRequest;
import com.ior.domain.dto.RegisterRequest;
import com.ior.domain.dto.DeletionRequest;
import com.ior.domain.dto.ResetPasswordRequest;
import com.ior.domain.dto.UpdateEmailRequest;
import com.ior.domain.dto.UpdatePasswordRequest;
import com.ior.domain.dto.UpdateProfileRequest;
import com.ior.domain.vo.Result;
import com.ior.service.IorUsersService;
import com.ior.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "包含注册、登录、验证码等接口")
public class IorUsersController {

    @Resource
    private IorUsersService iorUsersService;

    @Resource
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用邮箱验证码完成账户注册")
    public Result register(@RequestBody @Valid RegisterRequest request) {
        return iorUsersService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持密码登录和邮箱验证码登录")
    public Result login(@RequestBody @Valid LoginRequest request) {
        return iorUsersService.login(request);
    }

    @GetMapping("/sendcode")
    @Operation(summary = "发送邮箱验证码", description = "用于注册、登录或找回密码时的身份验证")
    public Result sendCode(
            @Parameter(description = "接收验证码的邮箱地址", required = true, example = "user@example.com") 
            @RequestParam String email,
            
            @Parameter(description = "验证码类型: register-注册, login-登录, reset_password-找回密码，change_email-重置邮箱", required = true, example = "register") 
            @RequestParam String type) {
        return iorUsersService.sendCode(email, type);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "修改昵称和头像")
    public Result updateProfile(@RequestHeader("Authorization") String token, @RequestBody @Valid UpdateProfileRequest request) {
        Long userId = extractUserId(token);
        return iorUsersService.updateProfile(userId, request);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "验证旧密码并设置新密码，成功后原 Token 失效")
    public Result updatePassword(@RequestHeader("Authorization") String token, @RequestBody @Valid UpdatePasswordRequest request) {
        Long userId = extractUserId(token);
        // 传入原始 token 字符串以便后续拉黑
        return iorUsersService.updatePassword(userId, request, token);
    }

    @PutMapping("/email")
    @Operation(summary = "修改邮箱", description = "需要新旧邮箱的双重验证码，成功后原 Token 失效")
    public Result updateEmail(@RequestHeader("Authorization") String token, @RequestBody @Valid UpdateEmailRequest request) {
        Long userId = extractUserId(token);
        return iorUsersService.updateEmail(userId, request, token);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "将当前 Token 加入黑名单，使其失效")
    public Result logout(@RequestHeader("Authorization") String token) {
        return iorUsersService.logout(token);
    }

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像", description = "上传图片作为用户头像，支持 JPG、PNG、GIF、WEBP、BMP 格式，最大 5MB")
    public Result uploadAvatar(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "头像文件", required = true)
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Long userId = extractUserId(token);
        return iorUsersService.uploadAvatar(userId, file);
    }

    @PostMapping("/deletion/request")
    @Operation(summary = "申请注销账户", description = "需要邮箱验证码验证身份，提交后进入7天冷静期")
    public Result requestDeletion(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid DeletionRequest request) {
        Long userId = extractUserId(token);
        return iorUsersService.requestDeletion(userId, request);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "重置密码", description = "通过邮箱验证码重置密码，无需登录")
    public Result resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return iorUsersService.resetPassword(request);
    }

    @GetMapping("/profile")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public Result getUserInfo(@RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        return iorUsersService.getUserInfo(userId);
    }

    /**
     * 辅助方法：从 Token 中提取用户 ID
     */
    private Long extractUserId(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token 不能为空");
        }
        // 1. 去除首尾所有空白字符
        token = token.trim();
        
        // 2. 如果以 Bearer 开头，则截取后面的部分并再次去空
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        
        if (token.isEmpty()) {
            throw new IllegalArgumentException("无效的 Token");
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        return ((Number) claims.get("userId")).longValue();
    }



}
