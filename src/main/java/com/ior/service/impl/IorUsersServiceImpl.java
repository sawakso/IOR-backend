package com.ior.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ior.domain.entity.IorUsers;
import com.ior.domain.vo.Result;
import com.ior.mapper.IorUsersMapper;
import com.ior.service.IorUsersService;
import com.ior.service.MailService;
import com.ior.strategy.VerificationCodeContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IorUsersServiceImpl extends ServiceImpl<IorUsersMapper, IorUsers> implements IorUsersService {
    
    private final VerificationCodeContext codeContext;
    private final MailService mailService;

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
}
