package com.ior.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ior.domain.dto.LoginRequest;
import com.ior.domain.dto.RegisterRequest;
import com.ior.domain.dto.UpdateEmailRequest;
import com.ior.domain.dto.UpdatePasswordRequest;
import com.ior.domain.dto.UpdateProfileRequest;
import com.ior.domain.entity.IorUserDeletionRequest;
import com.ior.domain.entity.IorUsers;
import com.ior.domain.vo.Result;
import com.ior.mapper.IorUserDeletionRequestMapper;
import com.ior.mapper.IorUsersMapper;
import com.ior.service.IorUsersService;
import com.ior.service.MailService;
import com.ior.service.R2StorageService;
import com.ior.strategy.VerificationCodeContext;
import com.ior.utils.JwtUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IorUsersServiceImpl extends ServiceImpl<IorUsersMapper, IorUsers> implements IorUsersService {
    
    private final VerificationCodeContext codeContext;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IorUserDeletionRequestMapper deletionRequestMapper;
    private final R2StorageService r2StorageService;

    @Override
    public Result sendCode(String email, String type) {
        // 1. 参数校验
        if (email == null) {
            return Result.error(400, "邮箱不能为空");
        }
        if (!Validator.isEmail(email)) {
            return Result.error(400, "邮箱格式不正确");
        }
        // 2. 查询用户是否已存在
        IorUsers existUser = lambdaQuery()
                .eq(IorUsers::getEmail, email)
                .one();
        if (existUser != null) {
            return Result.error(400, "该邮箱已注册");
        }
        // 3. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4. 保存验证码（自动根据配置切换到 Redis 或 数据库）
        // 使用 RedisConstants.CODE_TTL (2分钟) 或者自定义秒数
        codeContext.save(email, code, type, 120);
        // 5. 发送验证码（此处省略邮件发送逻辑）
        mailService.sendVerificationCode(email, code, type);  // type 一路透传
        System.out.println("发送给 " + email + " 的注册验证码: " + code);
        return Result.ok("验证码已发送");
    }

    @Override
    public Result register(RegisterRequest request) {
        // 1. 校验验证码
        String savedCode = codeContext.get(request.getEmail(), "register");
        if (savedCode == null || !savedCode.equals(request.getCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        // 2. 验证密码强度
        String passwordError = com.ior.utils.PasswordValidator.validate(request.getPassword());
        if (passwordError != null) {
            return Result.error(400, passwordError);
        }

        // 3. 检查账号、用户名、邮箱是否已存在
        LambdaQueryWrapper<IorUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUsers::getAccount, request.getAccount())
               .or().eq(IorUsers::getUsername, request.getUsername())
               .or().eq(IorUsers::getEmail, request.getEmail());
        if (this.count(wrapper) > 0) {
            return Result.error(400, "账号、用户名或邮箱已被占用");
        }

        // 4. 创建用户并加密密码
        IorUsers user = new IorUsers();
        user.setAccount(request.getAccount());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername()); // 默认昵称为用户名
        user.setRole("USER");
        user.setStatus(1);

        this.save(user);
        
        // 5. 删除已使用的验证码
        codeContext.remove(request.getEmail(), "register");
        
        return Result.ok("注册成功");
    }

    @Override
    public Result login(LoginRequest request) {
        // 1. 根据标识符查找用户
        LambdaQueryWrapper<IorUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUsers::getAccount, request.getIdentifier())
               .or().eq(IorUsers::getUsername, request.getIdentifier())
               .or().eq(IorUsers::getEmail, request.getIdentifier());
        IorUsers user = this.getOne(wrapper);

        if (user == null) {
            log.warn("登录失败：未找到标识符为 {} 的用户", request.getIdentifier());
            return Result.error(400, "用户不存在");
        }
        log.info("找到用户: ID={}, Username={}, Hash={}", user.getId(), user.getUsername(), user.getPasswordHash());

        // 2. 验证身份
        boolean isAuthenticated = false;
        if ("PASSWORD".equalsIgnoreCase(request.getLoginType())) {
            log.info("登录调试 - 输入密码: {}, 数据库哈希前10位: {}", request.getPassword(), 
                     user.getPasswordHash() != null ? user.getPasswordHash().substring(0, Math.min(10, user.getPasswordHash().length())) : "null");
            isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
            log.info("登录调试 - 密码比对结果: {}", isAuthenticated);
        } else if ("CODE".equalsIgnoreCase(request.getLoginType())) {
            String savedCode = codeContext.get(user.getEmail(), "login");
            if (savedCode != null && savedCode.equals(request.getCode())) {
                isAuthenticated = true;
                codeContext.remove(user.getEmail(), "login"); // 验证码一次性使用
            }
        }

        if (!isAuthenticated) {
            return Result.error(400, "凭证错误");
        }

        // 3. 如果用户处于冷静期（status=2），自动撤销注销申请
        if (user.getStatus() != null && user.getStatus() == 2) {
            log.info("用户 {} 处于冷静期，登录将自动撤销注销申请", user.getId());
            cancelDeletion(user.getId());
            // 重新查询用户状态
            user = this.getById(user.getId());
        }

        // 4. 更新最后登录时间和IP
        user.setLastLoginAt(LocalDateTime.now());
        // TODO: 获取真实 IP
        user.setLastLoginIp("127.0.0.1");
        user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
        this.updateById(user);

        // 5. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = jwtUtil.generateToken(claims);
        
        log.info("用户 {} 登录成功", user.getId());
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        // TODO: 实现注销逻辑（如将 Token 加入黑名单）
        return Result.ok("退出成功");
    }

    @Override
    public Result logout(String token) {
        if (token == null || token.isEmpty()) {
            return Result.error(400, "Token 不能为空");
        }
        
        // 提取纯净的 Token 字符串
        String pureToken = extractPureToken(token);
        
        // 将 Token 加入黑名单，使其失效
        jwtUtil.blacklistToken(pureToken);
        
        log.info("用户退出登录，Token 已加入黑名单");
        return Result.ok("退出成功");
    }

    @Override
    public Result updateProfile(Long userId, UpdateProfileRequest request) {
        IorUsers user = this.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
        this.updateById(user);
        return Result.ok("资料更新成功");
    }

    @Override
    public Result updatePassword(Long userId, UpdatePasswordRequest request, String token) {
        IorUsers user = this.getById(userId);
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return Result.error(400, "旧密码错误");
        }
        
        // 验证新密码强度
        String passwordError = com.ior.utils.PasswordValidator.validate(request.getNewPassword());
        if (passwordError != null) {
            return Result.error(400, passwordError);
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
        this.updateById(user);

        // 敏感操作后，将当前 Token 加入黑名单，强制重新登录
        jwtUtil.blacklistToken(extractPureToken(token));
        
        return Result.ok("密码修改成功，请重新登录");
    }

    @Override
    public Result updateEmail(Long userId, UpdateEmailRequest request, String token) {
        IorUsers user = this.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 1. 验证旧邮箱验证码
        String oldCode = codeContext.get(user.getEmail(), "change_email");
        if (oldCode == null || !oldCode.equals(request.getOldEmailCode())) {
            return Result.error(400, "旧邮箱验证码错误");
        }

        // 2. 验证新邮箱验证码
        String newCode = codeContext.get(request.getNewEmail(), "change_email");
        if (newCode == null || !newCode.equals(request.getNewEmailCode())) {
            return Result.error(400, "新邮箱验证码错误");
        }

        // 3. 检查新邮箱是否已被占用
        if (this.lambdaQuery().eq(IorUsers::getEmail, request.getNewEmail()).count() > 0) {
            return Result.error(400, "该邮箱已被注册");
        }

        // 4. 执行修改
        user.setEmail(request.getNewEmail());
        user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
        this.updateById(user);

        // 5. 清除验证码并拉黑当前 Token
        codeContext.remove(user.getEmail(), "change_email");
        codeContext.remove(request.getNewEmail(), "change_email");
        jwtUtil.blacklistToken(extractPureToken(token));

        return Result.ok("邮箱修改成功，请重新登录");
    }

    /**
     * 辅助方法：从 Header 值中提取纯净的 Token 字符串
     */
    private String extractPureToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader;
    }

    @Override
    @Transactional
    public Result requestDeletion(Long userId, com.ior.domain.dto.DeletionRequest request) {
        // 1. 查询用户信息
        IorUsers user = this.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 2. 验证邮箱验证码
        String savedCode = codeContext.get(user.getEmail(), "reset_password");
        if (savedCode == null || !savedCode.equals(request.getCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        // 3. 检查是否已经有待处理的注销申请
        LambdaQueryWrapper<IorUserDeletionRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUserDeletionRequest::getUserId, userId)
               .eq(IorUserDeletionRequest::getStatus, "PENDING");
        if (deletionRequestMapper.selectCount(wrapper) > 0) {
            return Result.error(400, "您已提交注销申请，正在冷静期中");
        }

        // 4. 创建注销申请记录
        IorUserDeletionRequest deletionRequest = new IorUserDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setRequestReason(request.getReason() != null ? request.getReason() : "");
        deletionRequest.setStatus("PENDING");
        deletionRequest.setRequestedAt(LocalDateTime.now());
        // 7天后完成
        deletionRequest.setCompletedAt(LocalDateTime.now().plusDays(7));
        
        deletionRequestMapper.insert(deletionRequest);

        // 5. 将用户状态改为“待注销”（冷静期）
        user.setStatus(2); // 2-待注销
        user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
        this.updateById(user);

        // 6. 清除已使用的验证码
        codeContext.remove(user.getEmail(), "reset_password");

        log.info("用户 {} 提交注销申请，进入7天冷静期", userId);
        return Result.ok("注销申请已提交，进入7天冷静期");
    }

    @Override
    @Transactional
    public Result cancelDeletion(Long userId) {
        // 1. 查找待处理的注销申请
        LambdaQueryWrapper<IorUserDeletionRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUserDeletionRequest::getUserId, userId)
               .eq(IorUserDeletionRequest::getStatus, "PENDING");
        IorUserDeletionRequest request = deletionRequestMapper.selectOne(wrapper);

        if (request != null) {
            // 2. 撤销申请
            request.setStatus("CANCELLED");
            request.setCancelledAt(LocalDateTime.now());
            deletionRequestMapper.updateById(request);

            // 3. 恢复用户正常状态
            IorUsers user = this.getById(userId);
            user.setStatus(1); // 1-正常
            user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
            this.updateById(user);
            
            return Result.ok("已撤销注销申请，账户恢复正常");
        }

        return Result.ok("当前无待处理的注销申请");
    }

    @Override
    public Result uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) {
        // 1. 验证文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的头像");
        }

        // 2. 验证文件大小（限制为 5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            return Result.error(400, "头像大小不能超过 5MB");
        }

        // 3. 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只支持图片格式（JPG、PNG、GIF 等）");
        }

        // 4. 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error(400, "文件名无效");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
            return Result.error(400, "不支持的图片格式，请使用 JPG、PNG、GIF、WEBP 或 BMP");
        }

        try {
            // 5. 查询用户当前头像
            IorUsers user = this.getById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            String oldAvatarUrl = user.getAvatarUrl();

            // 6. 上传新头像到 R2
            String newAvatarUrl = r2StorageService.uploadAvatar(file);

            // 7. 更新数据库中的头像 URL
            user.setAvatarUrl(newAvatarUrl);
            user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
            this.updateById(user);

            // 8. 删除旧头像（如果存在且不是默认头像）
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                r2StorageService.deleteFile(oldAvatarUrl);
                log.info("已删除旧头像: {}", oldAvatarUrl);
            }

            log.info("用户 {} 上传头像成功: {}", userId, newAvatarUrl);
            return Result.ok(newAvatarUrl);

        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result resetPassword(com.ior.domain.dto.ResetPasswordRequest request) {
        // 1. 验证邮箱格式
        if (!cn.hutool.core.lang.Validator.isEmail(request.getEmail())) {
            return Result.error(400, "邮箱格式不正确");
        }

        // 2. 查询用户是否存在
        IorUsers user = this.lambdaQuery()
                .eq(IorUsers::getEmail, request.getEmail())
                .one();
        
        if (user == null) {
            return Result.error(404, "该邮箱未注册");
        }

        // 3. 验证验证码
        String savedCode = codeContext.get(request.getEmail(), "reset_password");
        if (savedCode == null || !savedCode.equals(request.getCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        // 4. 验证新密码强度
        String passwordError = com.ior.utils.PasswordValidator.validate(request.getNewPassword());
        if (passwordError != null) {
            return Result.error(400, passwordError);
        }

        // 5. 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now()); // 显式更新时间
        this.updateById(user);

        // 6. 清除已使用的验证码
        codeContext.remove(request.getEmail(), "reset_password");

        log.info("用户 {} 重置密码成功", user.getId());
        return Result.ok("密码重置成功，请使用新密码登录");
    }

    @Override
    public Result getUserInfo(Long userId) {
        // 1. 查询用户信息
        IorUsers user = this.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 2. 转换为 VO 对象（隐藏敏感信息如 passwordHash）
        com.ior.domain.vo.UserInfoVO userInfoVO = com.ior.domain.vo.UserInfoVO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        log.debug("获取用户 {} 信息成功", userId);
        return Result.ok(userInfoVO);
    }
}
