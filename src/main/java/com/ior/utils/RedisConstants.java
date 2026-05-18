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
}