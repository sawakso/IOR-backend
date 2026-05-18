package com.ior.strategy;

/**
 * 验证码存储策略接口
 */
public interface VerificationCodeStrategy {

    /**
     * 保存验证码
     * @param email 邮箱
     * @param code 验证码
     * @param type 类型 (REGISTER, LOGIN, etc.)
     * @param expireSeconds 过期时间（秒）
     */
    void save(String email, String code, String type, long expireSeconds);

    /**
     * 获取验证码
     * @param email 邮箱
     * @param type 类型
     * @return 验证码
     */
    String get(String email, String type);

    /**
     * 删除验证码（验证成功后调用）
     * @param email 邮箱
     * @param type 类型
     */
    void remove(String email, String type);
}
