package com.ior.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ior.domain.entity.IorUsers;
import com.ior.mapper.IorUsersMapper;
import com.ior.service.IorUsersService;
import org.springframework.stereotype.Service;

@Service
public class UsersServiceImpl extends ServiceImpl<IorUsersMapper, IorUsers> implements IorUsersService {
}
