package com.ior.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ior.domain.dto.LoginRequest;
import com.ior.domain.dto.RegisterRequest;
import com.ior.domain.dto.UpdateEmailRequest;
import com.ior.domain.dto.UpdatePasswordRequest;
import com.ior.domain.dto.UpdateProfileRequest;
import com.ior.domain.entity.IorUsers;
import com.ior.domain.vo.Result;

public interface IorUsersService extends IService<IorUsers> {
    Result sendCode(String email, String type);

    Result register(RegisterRequest request);

    Result login(LoginRequest request);

    Result logout();

    /**
     * 更新基本资料（昵称、头像）
     */
    Result updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码
     */
    Result updatePassword(Long userId, UpdatePasswordRequest request, String token);

    /**
     * 修改邮箱（需双重验证）
     */
    Result updateEmail(Long userId, UpdateEmailRequest request, String token);

    /**
     * 申请注销账户（进入7天冷静期）
     */
    Result requestDeletion(Long userId, String reason);

    /**
     * 撤销注销申请（冷静期内登录即触发）
     */
    Result cancelDeletion(Long userId);
}
