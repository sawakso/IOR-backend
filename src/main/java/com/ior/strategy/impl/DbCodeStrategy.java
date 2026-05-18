package com.ior.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ior.domain.entity.IorVerificationCode;
import com.ior.mapper.IorVerificationCodeMapper;
import com.ior.strategy.VerificationCodeStrategy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("dbCodeStrategy")
public class DbCodeStrategy implements VerificationCodeStrategy {

    @Resource
    private IorVerificationCodeMapper codeMapper;

    @Override
    public void save(String email, String code, String type, long expireSeconds) {
        // 使用 LocalDateTime 自带的 plusSeconds 方法计算过期时间
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(expireSeconds);

        // 先删除旧的，再插入新的（保证唯一性）
        LambdaQueryWrapper<IorVerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IorVerificationCode::getEmail, email)
                    .eq(IorVerificationCode::getType, type);
        codeMapper.delete(queryWrapper);

        IorVerificationCode vc = new IorVerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setType(type);
        vc.setUsed(0);
        vc.setExpiresAt(expireTime);
        codeMapper.insert(vc);
    }

    @Override
    public String get(String email, String type) {
        LambdaQueryWrapper<IorVerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IorVerificationCode::getEmail, email)
                    .eq(IorVerificationCode::getType, type)
                    .eq(IorVerificationCode::getUsed, 0)
                    .gt(IorVerificationCode::getExpiresAt, LocalDateTime.now())
                    .orderByDesc(IorVerificationCode::getCreatedAt)
                    .last("LIMIT 1");
        IorVerificationCode vc = codeMapper.selectOne(queryWrapper);
        return vc != null ? vc.getCode() : null;
    }

    @Override
    public void remove(String email, String type) {
        LambdaQueryWrapper<IorVerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IorVerificationCode::getEmail, email)
                    .eq(IorVerificationCode::getType, type);
        codeMapper.delete(queryWrapper);
    }
}