package com.ior.utils;

import com.ior.utils.RedisConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.secret:ior_default_secret_key_for_jwt_token_generation}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认24小时
    private Long expiration;

    /**
     * 生成密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     * @param claims 自定义数据（如用户ID、角色等）
     * @return JWT 字符串
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 从 Token 中获取 Claims
     * @param token JWT 字符串
     * @return Claims 对象
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效（包含黑名单检查）
     */
    public boolean validateToken(String token) {
        try {
            // 1. 检查是否在黑名单中
            String key = RedisConstants.blacklistKey(token);
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(hasKey)) {
                System.out.println("[JWT校验] Token 已在黑名单中: " + token.substring(0, 10) + "...");
                return false;
            }
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            System.err.println("[JWT校验] 异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将 Token 加入黑名单（用于修改密码/注销后使旧 Token 失效）
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            long expiration = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (expiration > 0) {
                String key = RedisConstants.blacklistKey(token);
                // 存入 Redis，过期时间与 Token 剩余时间一致
                stringRedisTemplate.opsForValue().set(key, "invalid", expiration, java.util.concurrent.TimeUnit.MILLISECONDS);
                System.out.println("[JWT拉黑] 已将 Token 加入黑名单: " + key);
            }
        } catch (Exception e) {
            System.err.println("[JWT拉黑] 失败: " + e.getMessage());
        }
    }
}
