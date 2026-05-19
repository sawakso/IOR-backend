# Redis 缓存使用指南

## 📚 目录
1. [缓存策略概述](#缓存策略概述)
2. [Redis Key 规范](#redis-key-规范)
3. [使用示例](#使用示例)
4. [最佳实践](#最佳实践)

---

## 缓存策略概述

### Cache-Aside 模式（旁路缓存）

```
读操作流程：
┌─────────────┐
│  请求数据    │
└──────┬──────┘
       ↓
┌─────────────┐      命中     ┌──────────┐
│ 查询 Redis   │ ───────────→ │ 返回数据  │
└──────┬──────┘              └──────────┘
       │ 未命中
       ↓
┌─────────────┐      查到      ┌──────────┐
│ 查询 MySQL   │ ───────────→ │ 写入缓存  │
└──────┬──────┘              └────┬─────┘
       │                          │
       └──────────────────────────┘
                  ↓
          ┌──────────┐
          │ 返回数据  │
          └──────────┘


写操作流程：
┌─────────────┐
│  更新数据    │
└──────┬──────┘
       ↓
┌─────────────┐
│ 更新 MySQL   │ ← 先更新数据库
└──────┬──────┘
       ↓
┌─────────────┐
│ 删除缓存     │ ← 再删除缓存（不是更新！）
└──────┬──────┘
       ↓
┌─────────────┐
│  完成        │
└─────────────┘
```

**为什么删除而不是更新缓存？**
- ✅ 避免并发问题（多个请求同时更新可能导致脏数据）
- ✅ 懒加载策略（下次查询时自动重新加载最新数据）
- ✅ 简化逻辑（不需要考虑缓存更新的原子性）

---

## Redis Key 规范

所有 Key 都定义在 `RedisConstants` 类中，格式统一为 `ior:{模块}:{功能}:{参数}`

### 已定义的 Key

| 常量方法 | Key 格式 | 过期时间 | 说明 |
|---------|---------|---------|------|
| `codeKey(type, email)` | `ior:code:{type}:{email}` | 2分钟 | 验证码 |
| `blacklistKey(token)` | `ior:blacklist:token:{token}` | Token剩余时间 | Token黑名单 |
| `userInfoKey(userId)` | `ior:user:info:{userId}` | 30分钟 | 用户信息 |
| `postDetailKey(postId)` | `ior:post:detail:{postId}` | 10分钟 | 帖子详情 |
| `postListKey(catId, page, size)` | `ior:post:list:{catId}:{page}:{size}` | 5分钟 | 帖子列表 |
| `categoryListKey()` | `ior:category:list` | 60分钟 | 分类列表 |
| `tagHotKey()` | `ior:tag:hot` | 60分钟 | 热门标签 |
| `postVersionsKey(postId)` | `ior:post:versions:{postId}` | 10分钟 | 版本列表 |

---

## 使用示例

### 1. 缓存用户信息

```java
@Service
public class IorUsersServiceImpl extends ServiceImpl<IorUsersMapper, IorUsers> 
        implements IorUsersService {

    @Resource
    private RedisCacheHelper cacheHelper;

    /**
     * 获取用户信息（带缓存）
     */
    public Result getUserInfo(Long userId) {
        // ✅ 使用 getOrLoad 方法，自动处理缓存逻辑
        IorUsers user = cacheHelper.getOrLoad(
            RedisConstants.userInfoKey(userId),
            RedisConstants.USER_INFO_TTL,
            () -> this.getById(userId)  // Lambda 表达式：数据库查询
        );

        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 转换为 VO（不包含密码等敏感信息）
        UserInfoVO userInfoVO = convertToVO(user);
        return Result.ok(userInfoVO);
    }

    /**
     * 更新用户信息后删除缓存
     */
    public Result updateProfile(Long userId, UpdateProfileRequest request) {
        // 1. 更新数据库
        IorUsers user = this.getById(userId);
        user.setNickname(request.getNickname());
        this.updateById(user);

        // 2. 删除缓存（保证一致性）
        cacheHelper.invalidate(RedisConstants.userInfoKey(userId));

        return Result.ok("更新成功");
    }
}
```

### 2. 缓存帖子详情

```java
@Service
public class IorPostServiceImpl extends ServiceImpl<IorPostMapper, IorPost> 
        implements IorPostService {

    @Resource
    private RedisCacheHelper cacheHelper;
    
    @Resource
    private IorPostMediaMapper postMediaMapper;

    /**
     * 获取帖子详情（带缓存）
     */
    public Result getPostDetail(Long postId, Long currentUserId) {
        // 1. 从缓存或数据库获取帖子
        IorPost post = cacheHelper.getOrLoad(
            RedisConstants.postDetailKey(postId),
            RedisConstants.POST_DETAIL_TTL,
            () -> this.getById(postId)
        );

        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 2. 权限检查
        if ("PRIVATE".equals(post.getVisibility()) && 
            !post.getUserId().equals(currentUserId)) {
            return Result.error(403, "无权查看此帖子");
        }

        // 3. 查询媒体文件（不缓存，因为随版本变化）
        List<IorPostMedia> mediaList = postMediaMapper.selectList(
            new LambdaQueryWrapper<IorPostMedia>()
                .eq(IorPostMedia::getPostId, postId)
                .eq(IorPostMedia::getVersionId, post.getCurrentVersionId())
                .orderByAsc(IorPostMedia::getSortOrder)
        );

        // 4. 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("post", post);
        result.put("mediaList", mediaList);

        return Result.ok(result);
    }

    /**
     * 修改帖子后删除缓存
     */
    @Transactional
    public Result updatePost(Long postId, Long userId, ...) {
        // ... 省略版本管理逻辑 ...

        // 1. 更新数据库
        this.updateById(post);

        // 2. 删除帖子详情缓存
        cacheHelper.invalidate(RedisConstants.postDetailKey(postId));

        // 3. 删除相关列表缓存（因为列表可能包含该帖子）
        cacheHelper.invalidatePattern("ior:post:list:*");

        return Result.ok(newVersionNumber);
    }
}
```

### 3. 缓存帖子列表

```java
/**
 * 获取帖子列表（带缓存）
 */
public Result getPostList(Long categoryId, Integer page, Integer size, Long currentUserId) {
    // 生成缓存 Key
    String cacheKey = RedisConstants.postListKey(categoryId, page, size);

    // 从缓存或数据库获取
    List<IorPost> posts = cacheHelper.getOrLoad(
        cacheKey,
        RedisConstants.POST_LIST_TTL,
        () -> {
            // 数据库查询
            LambdaQueryWrapper<IorPost> wrapper = new LambdaQueryWrapper<>();
            
            // 分类筛选
            if (categoryId != null && categoryId > 0) {
                wrapper.eq(IorPost::getCategoryId, categoryId);
            }
            
            // 只查询正常状态的帖子
            wrapper.eq(IorPost::getStatus, 1)
                   .orderByDesc(IorPost::getCreatedAt)
                   .last("LIMIT " + size + " OFFSET " + (page - 1) * size);
            
            return this.list(wrapper);
        }
    );

    // 过滤私密帖子（非作者不可见）
    List<IorPost> filteredPosts = posts.stream()
        .filter(post -> 
            "PUBLIC".equals(post.getVisibility()) || 
            post.getUserId().equals(currentUserId)
        )
        .collect(Collectors.toList());

    return Result.ok(filteredPosts);
}
```

### 4. 缓存分类列表

```java
@Service
public class IorPostCategoryServiceImpl 
        extends ServiceImpl<IorPostCategoryMapper, IorPostCategory> 
        implements IorPostCategoryService {

    @Resource
    private RedisCacheHelper cacheHelper;

    /**
     * 获取所有分类（带缓存）
     */
    public Result getCategoryList() {
        List<IorPostCategory> categories = cacheHelper.getOrLoad(
            RedisConstants.categoryListKey(),
            RedisConstants.CATEGORY_LIST_TTL,
            () -> this.list(new LambdaQueryWrapper<IorPostCategory>()
                .eq(IorPostCategory::getIsActive, 1)
                .orderByAsc(IorPostCategory::getSortOrder)
            )
        );

        return Result.ok(categories);
    }

    /**
     * 管理员更新分类后删除缓存
     */
    public Result updateCategory(Long categoryId, ...) {
        // 1. 更新数据库
        this.updateById(category);

        // 2. 删除分类列表缓存
        cacheHelper.invalidate(RedisConstants.categoryListKey());

        return Result.ok("更新成功");
    }
}
```

---

## 最佳实践

### ✅ 推荐做法

1. **优先使用 `getOrLoad` 方法**
   ```java
   // ✅ 简洁明了，自动处理缓存逻辑
   User user = cacheHelper.getOrLoad(key, ttl, () -> mapper.selectById(id));
   ```

2. **写操作后务必删除缓存**
   ```java
   // ✅ 更新后立即删除缓存
   mapper.updateById(entity);
   cacheHelper.invalidate(cacheKey);
   ```

3. **合理设置过期时间**
   ```java
   // ✅ 根据数据变化频率设置
   - 用户信息：30分钟（变化少）
   - 帖子详情：10分钟（中等频率）
   - 帖子列表：5分钟（变化频繁）
   - 分类列表：60分钟（几乎不变）
   ```

4. **使用通配符批量删除**
   ```java
   // ✅ 帖子更新后，删除所有相关的列表缓存
   cacheHelper.invalidatePattern("ior:post:list:*");
   ```

### ❌ 避免的做法

1. **不要直接操作 RedisTemplate**
   ```java
   // ❌ 不推荐：手动序列化/反序列化
   String json = redisTemplate.opsForValue().get(key);
   User user = objectMapper.readValue(json, User.class);

   // ✅ 推荐：使用封装好的方法
   User user = cacheHelper.get(key, User.class);
   ```

2. **不要在缓存中存储敏感信息**
   ```java
   // ❌ 错误：缓存包含密码哈希
   cacheHelper.set(key, userWithPassword, ttl);

   // ✅ 正确：只缓存必要字段
   cacheHelper.set(key, userInfoVO, ttl);
   ```

3. **不要忘记删除缓存**
   ```java
   // ❌ 错误：更新后未删除缓存
   mapper.updateById(entity);
   // 忘记删除缓存，导致脏数据

   // ✅ 正确：更新后立即删除
   mapper.updateById(entity);
   cacheHelper.invalidate(cacheKey);
   ```

---

## 性能优化建议

### 1. 缓存预热

系统启动时预加载热点数据：

```java
@Component
public class CacheWarmup implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private RedisCacheHelper cacheHelper;
    
    @Resource
    private IorPostCategoryService categoryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 预热分类列表（几乎不变的数据）
        categoryService.getCategoryList();
        log.info("缓存预热完成");
    }
}
```

### 2. 监控缓存命中率

```java
// 在日志中观察缓存命中情况
// 缓存命中: ior:post:detail:123
// 缓存未命中，查询数据库: ior:post:detail:123
// 写入缓存: ior:post:detail:123, TTL: 10分钟
```

### 3. 避免缓存穿透

对于不存在的数据，可以缓存空值：

```java
public User getUser(Long userId) {
    String key = RedisConstants.userInfoKey(userId);
    
    User user = cacheHelper.getOrLoad(key, RedisConstants.USER_INFO_TTL, () -> {
        User dbUser = mapper.selectById(userId);
        // 如果数据库也没有，缓存空对象5分钟，避免频繁查询
        return dbUser != null ? dbUser : new User(); // 空对象
    });
    
    // 判断是否为空对象
    if (user.getId() == null) {
        return null;
    }
    
    return user;
}
```

---

## 总结

### 核心要点

1. **使用 `RedisCacheHelper` 工具类**，不要直接操作 `RedisTemplate`
2. **读操作**：使用 `getOrLoad()` 方法，自动实现 Cache-Aside 模式
3. **写操作**：先更新数据库，再调用 `invalidate()` 删除缓存
4. **Key 管理**：所有 Key 都在 `RedisConstants` 中定义，保持统一
5. **过期时间**：根据数据变化频率合理设置

### 缓存收益

- ✅ **降低数据库压力**：热点数据直接从 Redis 读取
- ✅ **提升响应速度**：Redis 读取速度比 MySQL 快 10-100 倍
- ✅ **提高系统吞吐量**：支持更高的并发访问

### 注意事项

- ⚠️ 缓存不是万能的，需要根据业务场景合理使用
- ⚠️ 注意缓存一致性问题，写操作后务必删除缓存
- ⚠️ 监控缓存命中率，及时调整缓存策略
