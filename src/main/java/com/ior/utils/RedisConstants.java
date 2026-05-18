package com.ior.utils;

public class RedisConstants {

    /** 验证码 key 前缀 */
    private static final String CODE_PREFIX = "ior:code:";

    /** 验证码默认过期时间（分钟） */
    public static final Long CODE_TTL = 2L;

    /**
     * 根据类型和邮箱生成 Redis key
     * @param type 验证码类型：REGISTER / LOGIN / CHANGE_EMAIL / RESET_PASSWORD
     * @param email 邮箱
     * @return ior:code:register:user@example.com
     */
    public static String codeKey(String type, String email) {
        return CODE_PREFIX + type.toLowerCase() + ":" + email;
    }

    /** Token 黑名单 key 前缀 */
    private static final String BLACKLIST_PREFIX = "ior:blacklist:token:";

    /**
     * 生成 Token 黑名单 Redis key
     * @param token JWT 字符串
     * @return ior:blacklist:token:<token>
     */
    public static String blacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }
}