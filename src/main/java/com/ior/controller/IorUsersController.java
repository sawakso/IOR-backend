package com.ior.controller;

import com.ior.domain.vo.Result;
import com.ior.service.IorUsersService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class IorUsersController {

    @Resource
    private IorUsersService iorUsersService;

    @GetMapping("/sendcode")
    public Result sendCode(@RequestParam String email) {
        return iorUsersService.sendCode(email);
    }


}
