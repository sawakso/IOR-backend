package com.ior.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ior.domain.entity.IorUsers;
import com.ior.domain.vo.Result;

public interface IorUsersService extends IService<IorUsers> {
    Result sendCode(String email, String type);
}
