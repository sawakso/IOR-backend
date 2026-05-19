# 生命流模块 - 当前状态总结

## ✅ 已完成的部分

### 1. 数据库表结构（4张表）

| 表名 | 说明 | SQL文件 | 状态 |
|------|------|---------|------|
| `ior_posts` | 帖子主表（文本内容） | `2_life_stream_optimized.sql` | ✅ 完整 |
| `ior_post_versions` | 帖子版本表（文本快照） | `2_life_stream_optimized.sql` | ✅ 完整 |
| `ior_post_media` | 媒体文件表（独立存储） | `2_life_stream_optimized.sql` | ✅ 完整 |
| `ior_post_categories` | 分类表 | `2_life_stream_optimized.sql` | ✅ 完整 |

### 2. Entity 实体类（4个）

- ✅ `IorPost.java` - 帖子主表实体
- ✅ `IorPostVersion.java` - 版本表实体
- ✅ `IorPostMedia.java` - 媒体文件实体
- ✅ `IorPostCategory.java` - 分类表实体

### 3. Mapper 接口（4个）

- ✅ `IorPostMapper.java`
- ✅ `IorPostVersionMapper.java`
- ✅ `IorPostMediaMapper.java`
- ✅ `IorPostCategoryMapper.java`

### 4. Service 层

- ✅ `IorPostService.java` - 接口定义
- ⚠️ `IorPostServiceImpl.java` - 部分完成（需要调整媒体文件逻辑）
- ❌ `IorPostCategoryService.java` - 待创建

### 5. Controller 层

- ✅ `IorPostController.java` - 帖子相关接口（需要调整）
- ❌ `IorPostCategoryController.java` - 待创建

### 6. Redis 缓存

- ✅ `RedisConstants.java` - Key 常量定义完整
- ✅ `RedisCacheHelper.java` - Cache-Aside 工具类
- ⚠️ 未在 Service 中集成使用

---

## 🔧 需要调整的部分

### 问题：当前实现使用 JSON 存储媒体 URL

**现状**：
```java
// IorPost 实体中有这个字段
private String mediaUrls;  // JSON 格式

// 创建帖子时
IorPost post = IorPost.builder()
    .mediaUrls(mediaUrls)  // ← 存 JSON
    .build();
```

**问题**：
- 与我们设计的"媒体文件独立建表"方案不一致
- 无法利用 `ior_post_media` 表的优势

---

## 📋 下一步行动计划

### 方案 A：继续使用 JSON（快速实现）✅ 推荐初期使用

**优点**：
- 实现简单，改动小
- 适合快速验证产品

**需要做的**：
1. 保持当前代码不变
2. 执行 SQL 脚本时，可以暂时不创建 `ior_post_media` 表
3. 后期需要时再迁移到独立表

**适用场景**：MVP 阶段、原型验证

---

### 方案 B：使用独立媒体表（规范实现）🏆 推荐生产环境

**优点**：
- 数据结构规范
- 支持复杂的媒体管理
- 便于扩展和维护

**需要做的**：

#### 1. 修改 IorPost 实体
```java
// 删除这个字段
// private String mediaUrls;
```

#### 2. 修改创建帖子逻辑
```java
@Transactional
public Result createPost(Long userId, String title, String content, 
                        List<MultipartFile> files, ...) {
    // 1. 创建帖子（不存媒体URL）
    IorPost post = IorPost.builder()
            .userId(userId)
            .title(title)
            .content(content)
            // .mediaUrls(...) ← 删除这行
            .versionCount(1)
            .build();
    this.save(post);
    
    // 2. 创建版本 v1
    IorPostVersion version = IorPostVersion.builder()
            .postId(post.getId())
            .versionNumber(1)
            .title(title)
            .content(content)
            // .mediaUrls(...) ← 删除这行
            .changeSummary("初始版本")
            .createdBy(userId)
            .build();
    postVersionMapper.insert(version);
    
    // 3. 上传并保存媒体文件到独立表
    if (files != null && !files.isEmpty()) {
        int sortOrder = 0;
        for (MultipartFile file : files) {
            // 上传到 R2
            String fileUrl = r2StorageService.uploadFile(file);
            
            // 保存到媒体表
            IorPostMedia media = IorPostMedia.builder()
                    .postId(post.getId())
                    .versionId(version.getId())  // ⭐ 关联版本
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType().startsWith("image/") ? "IMAGE" : "VIDEO")
                    .fileFormat(getFileExtension(file.getOriginalFilename()))
                    .fileSize(file.getSize())
                    .sortOrder(sortOrder++)
                    .build();
            postMediaMapper.insert(media);
        }
    }
    
    // 4. 更新当前版本ID
    post.setCurrentVersionId(version.getId());
    this.updateById(post);
    
    return Result.ok(post.getId());
}
```

#### 3. 修改查询帖子详情逻辑
```java
public Result getPostDetail(Long postId, Long currentUserId) {
    // 1. 查询帖子
    IorPost post = this.getById(postId);
    if (post == null) {
        return Result.error(404, "帖子不存在");
    }
    
    // 2. 权限检查
    if ("PRIVATE".equals(post.getVisibility()) && 
        !post.getUserId().equals(currentUserId)) {
        return Result.error(403, "无权查看此帖子");
    }
    
    // 3. 查询当前版本的媒体文件
    List<IorPostMedia> mediaList = postMediaMapper.selectList(
        new LambdaQueryWrapper<IorPostMedia>()
            .eq(IorPostMedia::getPostId, postId)
            .eq(IorPostMedia::getVersionId, post.getCurrentVersionId())
            .eq(IorPostMedia::getStatus, 1)
            .orderByAsc(IorPostMedia::getSortOrder)
    );
    
    // 4. 组装返回数据
    Map<String, Object> result = new HashMap<>();
    result.put("post", post);
    result.put("mediaList", mediaList);
    
    return Result.ok(result);
}
```

#### 4. 修改更新帖子逻辑
```java
@Transactional
public Result updatePost(Long postId, Long userId, String title, 
                        String content, List<MultipartFile> newFiles,
                        List<Long> deleteMediaIds, String changeSummary) {
    // ... 省略版本创建逻辑 ...
    
    // 1. 创建新版本
    IorPostVersion newVersion = ...;
    postVersionMapper.insert(newVersion);
    
    // 2. 复制旧版本的媒体文件到新版本
    List<IorPostMedia> oldMediaList = postMediaMapper.selectList(
        new LambdaQueryWrapper<IorPostMedia>()
            .eq(IorPostMedia::getPostId, postId)
            .eq(IorPostMedia::getVersionId, post.getCurrentVersionId())
    );
    
    for (IorPostMedia oldMedia : oldMediaList) {
        // 如果不在删除列表中，则复制到新版本
        if (!deleteMediaIds.contains(oldMedia.getId())) {
            IorPostMedia newMedia = IorPostMedia.builder()
                    .postId(postId)
                    .versionId(newVersion.getId())  // ⭐ 关联新版本
                    .fileUrl(oldMedia.getFileUrl())
                    .fileType(oldMedia.getFileType())
                    .fileFormat(oldMedia.getFileFormat())
                    .fileSize(oldMedia.getFileSize())
                    .width(oldMedia.getWidth())
                    .height(oldMedia.getHeight())
                    .sortOrder(oldMedia.getSortOrder())
                    .build();
            postMediaMapper.insert(newMedia);
        }
    }
    
    // 3. 添加新上传的媒体文件
    if (newFiles != null && !newFiles.isEmpty()) {
        int sortOrder = oldMediaList.size();
        for (MultipartFile file : newFiles) {
            String fileUrl = r2StorageService.uploadFile(file);
            
            IorPostMedia media = IorPostMedia.builder()
                    .postId(postId)
                    .versionId(newVersion.getId())
                    .fileUrl(fileUrl)
                    .fileType(...)
                    .fileFormat(...)
                    .fileSize(file.getSize())
                    .sortOrder(sortOrder++)
                    .build();
            postMediaMapper.insert(media);
        }
    }
    
    // 4. 更新帖子主表
    post.setTitle(title);
    post.setContent(content);
    post.setVersionCount(newVersionNumber);
    post.setCurrentVersionId(newVersion.getId());
    this.updateById(post);
    
    return Result.ok(newVersionNumber);
}
```

---

## 🎯 我的建议

### 现阶段（开发初期）：**方案 A - 使用 JSON**

**理由**：
1. 您已经写好了大部分代码
2. 可以快速测试核心功能（版本管理）
3. 媒体文件管理可以后期优化

**实施步骤**：
1. 执行 `2_life_stream_optimized.sql` 时，注释掉 `ior_post_media` 表的创建
2. 保持当前代码不变
3. 先跑通核心流程：创建→修改→版本管理

### 后期（产品验证后）：**迁移到方案 B**

**时机**：
- 用户量增长
- 需要更复杂的媒体管理功能
- 需要统计和分析媒体数据

**迁移方案**：
1. 编写数据迁移脚本（从 JSON 提取到独立表）
2. 逐步替换 Service 层逻辑
3. 灰度发布，确保平稳过渡

---

## 📝 立即可以做的事

### 1. 执行数据库脚本

```bash
mysql -u root -p ior < sql/2_life_stream_optimized.sql
```

### 2. 创建分类 Service（可选，如果需要分类管理）

```java
@Service
public class IorPostCategoryService 
        extends ServiceImpl<IorPostCategoryMapper, IorPostCategory> {
    
    public Result getCategoryList() {
        List<IorPostCategory> categories = this.list(
            new LambdaQueryWrapper<IorPostCategory>()
                .eq(IorPostCategory::getIsActive, 1)
                .orderByAsc(IorPostCategory::getSortOrder)
        );
        return Result.ok(categories);
    }
}
```

### 3. 在 Controller 中集成 Redis 缓存

参考 `redis_cache_guide.md` 中的示例

---

## ❓ 您想怎么做？

**选项 1**：保持现状，先用 JSON 方案快速开发  
**选项 2**：我帮您重构为独立媒体表方案  
**选项 3**：先创建分类管理的 Service 和 Controller  

请告诉我您的选择！
