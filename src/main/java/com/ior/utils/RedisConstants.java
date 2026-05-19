package com.ior.utils;

public class RedisConstants {

    // ==================== 验证码相关 ====================
    
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

    // ==================== Token 黑名单 ====================

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

    // ==================== 用户信息缓存 ====================

    /** 用户信息 key 前缀 */
    private static final String USER_INFO_PREFIX = "ior:user:info:";

    /** 用户信息过期时间（分钟） */
    public static final Long USER_INFO_TTL = 30L;

    /**
     * 生成用户信息缓存 key
     * @param userId 用户ID
     * @return ior:user:info:123
     */
    public static String userInfoKey(Long userId) {
        return USER_INFO_PREFIX + userId;
    }

    // ==================== 帖子详情缓存 ====================

    /** 帖子详情 key 前缀 */
    private static final String POST_DETAIL_PREFIX = "ior:post:detail:";

    /** 帖子详情过期时间（分钟） */
    public static final Long POST_DETAIL_TTL = 10L;

    /**
     * 生成帖子详情缓存 key
     * @param postId 帖子ID
     * @return ior:post:detail:456
     */
    public static String postDetailKey(Long postId) {
        return POST_DETAIL_PREFIX + postId;
    }

    // ==================== 帖子列表缓存 ====================

    /** 帖子列表 key 前缀 */
    private static final String POST_LIST_PREFIX = "ior:post:list:";

    /** 帖子列表过期时间（分钟） */
    public static final Long POST_LIST_TTL = 5L;

    /**
     * 生成帖子列表缓存 key
     * @param categoryId 分类ID（0表示全部）
     * @param page 页码
     * @param size 每页数量
     * @return ior:post:list:1:1:20
     */
    public static String postListKey(Long categoryId, Integer page, Integer size) {
        return POST_LIST_PREFIX + categoryId + ":" + page + ":" + size;
    }

    // ==================== 分类列表缓存 ====================

    /** 分类列表 key */
    private static final String CATEGORY_LIST_KEY = "ior:category:list";

    /** 分类列表过期时间（分钟） */
    public static final Long CATEGORY_LIST_TTL = 60L;

    /**
     * 获取分类列表缓存 key
     * @return ior:category:list
     */
    public static String categoryListKey() {
        return CATEGORY_LIST_KEY;
    }

    // ==================== 热门标签缓存 ====================

    /** 热门标签 key */
    private static final String TAG_HOT_KEY = "ior:tag:hot";

    /** 热门标签过期时间（分钟） */
    public static final Long TAG_HOT_TTL = 60L;

    /**
     * 获取热门标签缓存 key
     * @return ior:tag:hot
     */
    public static String tagHotKey() {
        return TAG_HOT_KEY;
    }

    // ==================== 帖子版本列表缓存 ====================

    /** 帖子版本列表 key 前缀 */
    private static final String POST_VERSIONS_PREFIX = "ior:post:versions:";

    /** 帖子版本列表过期时间（分钟） */
    public static final Long POST_VERSIONS_TTL = 10L;

    /**
     * 生成帖子版本列表缓存 key
     * @param postId 帖子ID
     * @return ior:post:versions:456
     */
    public static String postVersionsKey(Long postId) {
        return POST_VERSIONS_PREFIX + postId;
    }
}