package com.ior.utils;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Component
public class JwtHelper {

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 从当前请求的 Header 中获取用户 ID
     */
    public Long getCurrentUserId() {
        String token = getTokenFromRequest();
        if (token == null) {
            return null;
        }
        Claims claims = jwtUtil.getClaimsFromToken(token);
        return ((Number) Objects.requireNonNull(claims.get("userId"))).longValue();
    }

    /**
     * 从当前请求的 Header 中获取用户角色
     */
    public String getCurrentUserRole() {
        String token = getTokenFromRequest();
        if (token == null) {
            return null;
        }
        Claims claims = jwtUtil.getClaimsFromToken(token);
        return (String) claims.get("role");
    }

    /**
     * 从 Token 字符串中获取用户 ID
     */
    public Long getUserIdByToken(String token) {
        if (token == null) {
            return null;
        }
        Claims claims = jwtUtil.getClaimsFromToken(token);
        return ((Number) Objects.requireNonNull(claims.get("userId"))).longValue();
    }

    /**
     * 从 Request 中提取 Bearer Token
     */
    private String getTokenFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            String header = attributes.getRequest().getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        } catch (Exception e) {
            // 忽略非 Web 环境下的调用
        }
        return null;
    }
}
