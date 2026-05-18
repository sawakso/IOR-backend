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
import com.ior.strategy.VerificationCodeContext;
import com.ior.utils.JwtUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IorUsersServiceImpl extends ServiceImpl<IorUsersMapper, IorUsers> implements IorUsersService {
    
    private final VerificationCodeContext codeContext;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IorUserDeletionRequestMapper deletionRequestMapper;

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

        // 2. 检查账号、用户名、邮箱是否已存在
        LambdaQueryWrapper<IorUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUsers::getAccount, request.getAccount())
               .or().eq(IorUsers::getUsername, request.getUsername())
               .or().eq(IorUsers::getEmail, request.getEmail());
        if (this.count(wrapper) > 0) {
            return Result.error(400, "账号、用户名或邮箱已被占用");
        }

        // 3. 创建用户并加密密码
        IorUsers user = new IorUsers();
        user.setAccount(request.getAccount());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername()); // 默认昵称为用户名
        user.setRole("USER");
        user.setStatus(1);

        this.save(user);
        
        // 4. 删除已使用的验证码
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
            return Result.error(400, "用户不存在");
        }

        // 2. 验证身份
        boolean isAuthenticated = false;
        if ("PASSWORD".equalsIgnoreCase(request.getLoginType())) {
            isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
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

        // 3. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = jwtUtil.generateToken(claims);
        
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        // TODO: 实现注销逻辑（如将 Token 加入黑名单）
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
        
        this.updateById(user);
        return Result.ok("资料更新成功");
    }

    @Override
    public Result updatePassword(Long userId, UpdatePasswordRequest request) {
        IorUsers user = this.getById(userId);
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return Result.error(400, "旧密码错误");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        this.updateById(user);
        return Result.ok("密码修改成功");
    }

    @Override
    public Result updateEmail(Long userId, UpdateEmailRequest request) {
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
        this.updateById(user);

        // 5. 清除验证码
        codeContext.remove(user.getEmail(), "change_email");
        codeContext.remove(request.getNewEmail(), "change_email");

        return Result.ok("邮箱修改成功");
    }

    @Override
    @Transactional
    public Result requestDeletion(Long userId, String reason) {
        // 1. 检查是否已经有待处理的注销申请
        LambdaQueryWrapper<IorUserDeletionRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorUserDeletionRequest::getUserId, userId)
               .eq(IorUserDeletionRequest::getStatus, "PENDING");
        if (deletionRequestMapper.selectCount(wrapper) > 0) {
            return Result.error(400, "您已提交注销申请，正在冷静期中");
        }

        // 2. 创建注销申请记录
        IorUserDeletionRequest request = new IorUserDeletionRequest();
        request.setUserId(userId);
        request.setRequestReason(reason != null ? reason : "");
        request.setStatus("PENDING");
        request.setRequestedAt(LocalDateTime.now());
        // 7天后完成
        request.setCompletedAt(LocalDateTime.now().plusDays(7));
        
        deletionRequestMapper.insert(request);

        // 3. 将用户状态改为“待注销”（冷静期）
        IorUsers user = this.getById(userId);
        user.setStatus(2); // 2-待注销
        this.updateById(user);

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
            this.updateById(user);
            
            return Result.ok("已撤销注销申请，账户恢复正常");
        }

        return Result.ok("当前无待处理的注销申请");
    }
}
