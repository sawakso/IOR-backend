# Git 式版本控制实现详解

## 📚 目录
1. [核心概念](#核心概念)
2. [数据存储方案](#数据存储方案)
3. [实现原理](#实现原理)
4. [代码流程](#代码流程)
5. [使用示例](#使用示例)
6. [性能优化](#性能优化)

---

## 核心概念

### Git 版本控制的特点

Git 的版本控制有以下几个核心特点，我们在生命流模块中实现了类似机制：

| Git 概念 | 生命流对应 | 说明 |
|---------|----------|------|
| Commit | 版本（Version） | 每次修改生成一个新版本 |
| Snapshot | 内容快照 | 保存完整的内容副本 |
| HEAD | current_version_id | 指向当前最新版本 |
| History | ior_post_versions 表 | 存储所有历史版本 |
| Restore | 恢复版本 | 可以回退到任意历史版本 |

### 为什么选择快照方式？

**快照方式（Snapshot）** vs **增量方式（Delta）**

```
快照方式（我们采用的）：
v1: { title: "标题1", content: "内容1" }
v2: { title: "标题2", content: "内容2" }  ← 完整保存
v3: { title: "标题3", content: "内容3" }  ← 完整保存

优点：
✅ 查询速度快（直接读取，无需计算）
✅ 实现简单（不需要复杂的 diff 算法）
✅ 易于恢复（直接复制即可）
✅ 版本对比容易（两个完整对象对比）

缺点：
❌ 存储空间较大（但现代存储成本低）


增量方式（Git 内部使用）：
v1: { title: "标题1", content: "内容1" }
v2: { title: "+标题2", content: "-内容1\n+内容2" }  ← 只保存差异
v3: { title: "+标题3", content: "-内容2\n+内容3" }  ← 只保存差异

优点：
✅ 节省存储空间

缺点：
❌ 查询慢（需要回溯计算）
❌ 实现复杂（需要 diff/patch 算法）
❌ 恢复困难（需要从 v1 开始逐步应用差异）
```

**结论**：对于中小型项目，快照方式是更好的选择！

---

## 数据存储方案

### 数据库表结构

#### 1. 帖子主表（ior_posts）- 只存当前状态

```
┌─────────────────────────────────────────┐
│ ior_posts (帖子主表)                     │
├─────────────────────────────────────────┤
│ id              | 1                     │
│ user_id         | 100                   │
│ title           | "我的第三版标题"       │ ← 当前版本的标题
│ content         | "这是最新的内容"       │ ← 当前版本的内容
│ media_urls      | ["url3.jpg"]          │ ← 当前版本的媒体
│ version_count   | 3                     │ ← 总共有3个版本
│ current_ver_id  | 303                   │ ← 指向最新版本ID
└─────────────────────────────────────────┘
```

**作用**：
- 快速查询帖子的当前状态
- 避免每次都去版本表查询最新版本
- 冗余设计，提升查询性能

#### 2. 帖子版本表（ior_post_versions）- 存所有历史版本 ⭐核心

```
┌──────────────────────────────────────────────────┐
│ ior_post_versions (版本历史表)                    │
├──────┬────────┬──────┬──────────┬────────────────┤
│ id   │ post_id│ ver  │ title    │ content        │
├──────┼────────┼──────┼──────────┼────────────────┤
│ 301  │ 1      │ 1    │ 标题1    │ 内容1          │ ← v1 快照
│ 302  │ 1      │ 2    │ 标题2    │ 内容2          │ ← v2 快照
│ 303  │ 1      │ 3    │ 标题3    │ 内容3          │ ← v3 快照（当前）
└──────┴────────┴──────┴──────────┴────────────────┘
```

**作用**：
- 保存所有历史版本的完整快照
- 支持查看历史版本
- 支持恢复到任意版本

### 数据关系图

```
用户修改帖子
    ↓
┌─────────────────────────────────────┐
│ 1. 读取当前帖子内容                   │
│    SELECT * FROM ior_posts           │
│    WHERE id = 1                      │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2. 创建新版本记录                     │
│    INSERT INTO ior_post_versions     │
│    (post_id, version_number, ...)    │
│    VALUES (1, 4, ...)                │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3. 更新帖子主表                      │
│    UPDATE ior_posts                  │
│    SET title = '新标题',             │
│        content = '新内容',           │
│        version_count = 4,            │
│        current_version_id = 304      │
│    WHERE id = 1                      │
└─────────────────────────────────────┘
```

---

## 实现原理

### 1. 创建帖子（初始版本 v1）

```java
@Transactional
public Result createPost(...) {
    // Step 1: 创建帖子主记录
    IorPost post = new IorPost();
    post.setTitle(title);
    post.setContent(content);
    post.setVersionCount(1);  // 初始版本数为1
    save(post);
    
    // Step 2: 创建第一个版本（v1）
    IorPostVersion version = new IorPostVersion();
    version.setPostId(post.getId());
    version.setVersionNumber(1);  // 版本号从1开始
    version.setTitle(title);       // 保存完整内容快照
    version.setContent(content);
    version.setChangeSummary("初始版本");
    postVersionMapper.insert(version);
    
    // Step 3: 关联当前版本ID
    post.setCurrentVersionId(version.getId());
    updateById(post);
    
    return Result.ok(post.getId());
}
```

### 2. 修改帖子（生成新版本）⭐核心逻辑

```java
@Transactional
public Result updatePost(Long postId, Long userId, ...) {
    // Step 1: 查询帖子
    IorPost post = getById(postId);
    
    // Step 2: 获取当前最大版本号
    IorPostVersion latestVersion = postVersionMapper.selectOne(
        new LambdaQueryWrapper<IorPostVersion>()
            .eq(IorPostVersion::getPostId, postId)
            .orderByDesc(IorPostVersion::getVersionNumber)
            .last("LIMIT 1")
    );
    int newVersionNumber = latestVersion.getVersionNumber() + 1;  // v2, v3, v4...
    
    // Step 3: 创建新版本（保存新内容的快照）
    IorPostVersion newVersion = new IorPostVersion();
    newVersion.setPostId(postId);
    newVersion.setVersionNumber(newVersionNumber);  // 递增版本号
    newVersion.setTitle(newTitle);    // 保存新的完整内容
    newVersion.setContent(newContent);
    newVersion.setChangeSummary(changeSummary);  // 用户填写的修改说明
    postVersionMapper.insert(newVersion);
    
    // Step 4: 更新帖子主表
    post.setTitle(newTitle);
    post.setContent(newContent);
    post.setVersionCount(newVersionNumber);  // 版本号+1
    post.setCurrentVersionId(newVersion.getId());  // 指向新版本
    updateById(post);
    
    return Result.ok(newVersionNumber);
}
```

### 3. 查看版本列表

```java
public Result getVersionList(Long postId, Long userId) {
    // 查询该帖子的所有版本，按版本号倒序排列
    List<IorPostVersion> versions = postVersionMapper.selectList(
        new LambdaQueryWrapper<IorPostVersion>()
            .eq(IorPostVersion::getPostId, postId)
            .orderByDesc(IorPostVersion::getVersionNumber)
    );
    
    // 返回版本列表
    // [
    //   { versionNumber: 5, createdAt: "...", changeSummary: "..." },
    //   { versionNumber: 4, createdAt: "...", changeSummary: "..." },
    //   ...
    // ]
    return Result.ok(versions);
}
```

### 4. 恢复到指定版本 ⭐Git-like 功能

```java
@Transactional
public Result restoreVersion(Long postId, Long versionId, Long userId) {
    // Step 1: 查询目标版本
    IorPostVersion targetVersion = postVersionMapper.selectById(versionId);
    
    // Step 2: 获取当前最大版本号
    IorPost post = getById(postId);
    int newVersionNumber = post.getVersionCount() + 1;
    
    // Step 3: 创建新版本（内容与目标版本相同）
    IorPostVersion newVersion = new IorPostVersion();
    newVersion.setPostId(postId);
    newVersion.setVersionNumber(newVersionNumber);  // 新版本号
    newVersion.setTitle(targetVersion.getTitle());   // 复制目标版本的内容
    newVersion.setContent(targetVersion.getContent());
    newVersion.setMediaUrls(targetVersion.getMediaUrls());
    newVersion.setChangeSummary("恢复到版本 v" + targetVersion.getVersionNumber());
    postVersionMapper.insert(newVersion);
    
    // Step 4: 更新帖子主表
    post.setTitle(targetVersion.getTitle());
    post.setContent(targetVersion.getContent());
    post.setVersionCount(newVersionNumber);
    post.setCurrentVersionId(newVersion.getId());
    updateById(post);
    
    return Result.ok(newVersionNumber);
}
```

**关键点**：
- 恢复操作本身也生成一个新版本
- 例如：当前 v5，恢复到 v2，会生成 v6（内容与 v2 相同）
- 这样保持了版本历史的线性完整性

---

## 代码流程

### 完整的版本生命周期

```
创建帖子
   ↓
v1 (初始版本)
   ↓
用户修改 → 生成 v2
   ↓
用户修改 → 生成 v3
   ↓
用户查看版本列表 → [v3, v2, v1]
   ↓
用户查看 v1 详情 → 显示 v1 的完整内容
   ↓
用户恢复到 v1 → 生成 v4（内容与 v1 相同）
   ↓
当前状态：v4 是最新版本
历史版本：v1, v2, v3, v4 全部保留
```

### 事务保证

所有版本操作都使用 `@Transactional` 注解，确保：
- ✅ 版本记录和帖子主表同时更新
- ✅ 任何一步失败都会回滚
- ✅ 数据一致性得到保证

---

## 使用示例

### 1. 创建帖子

```bash
POST /post/create
Authorization: Bearer YOUR_TOKEN

title=我的第一篇生命流
content=今天天气真好，心情不错
visibility=PUBLIC
tags=日常,心情
categoryId=1
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 123  // 帖子ID
}
```

**数据库状态**：
```
ior_posts:
  id: 123
  title: "我的第一篇生命流"
  version_count: 1
  current_version_id: 1

ior_post_versions:
  id: 1
  post_id: 123
  version_number: 1
  title: "我的第一篇生命流"
  content: "今天天气真好，心情不错"
  change_summary: "初始版本"
```

### 2. 修改帖子

```bash
PUT /post/123
Authorization: Bearer YOUR_TOKEN

title=我的第一篇生命流（修改版）
content=今天天气真的很好！阳光明媚
changeSummary=修正了错别字，增加了描述
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 2  // 新版本号
}
```

**数据库状态**：
```
ior_posts:
  id: 123
  title: "我的第一篇生命流（修改版）"  ← 已更新
  content: "今天天气真的很好！阳光明媚"  ← 已更新
  version_count: 2  ← 已增加
  current_version_id: 2  ← 指向新版本

ior_post_versions:
  id: 1  ← v1 保持不变
  version_number: 1
  title: "我的第一篇生命流"
  content: "今天天气真好，心情不错"
  
  id: 2  ← 新增 v2
  version_number: 2
  title: "我的第一篇生命流（修改版）"
  content: "今天天气真的很好！阳光明媚"
  change_summary: "修正了错别字，增加了描述"
```

### 3. 查看版本列表

```bash
GET /post/123/versions
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 2,
      "versionNumber": 2,
      "changeSummary": "修正了错别字，增加了描述",
      "createdAt": "2026-05-19T11:00:00"
    },
    {
      "id": 1,
      "versionNumber": 1,
      "changeSummary": "初始版本",
      "createdAt": "2026-05-19T10:00:00"
    }
  ]
}
```

### 4. 恢复到 v1

```bash
POST /post/123/versions/1/restore
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 3  // 新版本号
}
```

**数据库状态**：
```
ior_posts:
  id: 123
  title: "我的第一篇生命流"  ← 恢复到 v1 的内容
  content: "今天天气真好，心情不错"  ← 恢复到 v1 的内容
  version_count: 3  ← 增加到3
  current_version_id: 3  ← 指向新版本

ior_post_versions:
  id: 1  ← v1
  version_number: 1
  
  id: 2  ← v2
  version_number: 2
  
  id: 3  ← v3（新）
  version_number: 3
  title: "我的第一篇生命流"  ← 与 v1 相同
  content: "今天天气真好，心情不错"  ← 与 v1 相同
  change_summary: "恢复到版本 v1"
```

---

## 性能优化

### 1. 索引优化

```sql
-- 版本表的关键索引
INDEX idx_post_id (post_id)                    -- 快速查询某帖子的所有版本
INDEX idx_post_version (post_id, version_number) -- 联合索引，加速排序
UNIQUE KEY uk_post_version (post_id, version_number) -- 防止版本号重复
```

### 2. 查询优化

```java
// ❌ 不好的做法：查询所有字段
SELECT * FROM ior_post_versions WHERE post_id = 123;

// ✅ 好的做法：只查询需要的字段（版本列表场景）
SELECT id, version_number, change_summary, created_at 
FROM ior_post_versions 
WHERE post_id = 123 
ORDER BY version_number DESC;
```

### 3. 分页查询（当版本很多时）

```java
// 如果版本数量超过100，建议分页
Page<IorPostVersion> page = new Page<>(currentPage, pageSize);
LambdaQueryWrapper<IorPostVersion> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(IorPostVersion::getPostId, postId)
       .orderByDesc(IorPostVersion::getVersionNumber);
       
IPage<IorPostVersion> result = postVersionMapper.selectPage(page, wrapper);
```

### 4. 缓存策略

```java
// 热点数据缓存到 Redis
// Key: ior:post:current:{postId}
// Value: 帖子当前版本的JSON
@Cacheable(value = "postCurrent", key = "#postId")
public IorPost getCurrentPost(Long postId) {
    return getById(postId);
}
```

### 5. 存储空间优化（可选）

如果担心存储空间，可以考虑：

**方案 A：压缩存储**
```java
// 使用 GZIP 压缩内容后再存储
String compressedContent = compress(content);
version.setContent(compressedContent);

// 读取时解压
String content = decompress(version.getContent());
```

**方案 B：定期清理旧版本**
```java
// 保留最近50个版本，删除更早的版本
DELETE FROM ior_post_versions 
WHERE post_id = 123 
  AND version_number < (
    SELECT MAX(version_number) - 50 
    FROM ior_post_versions 
    WHERE post_id = 123
  );
```

---

## 总结

### Git 式版本控制的核心要点

1. **快照存储**：每次修改保存完整内容副本
2. **版本号递增**：v1, v2, v3... 线性增长
3. **历史不可变**：已创建的版本永不修改
4. **可回溯**：可以恢复到任意历史版本
5. **双表设计**：
   - 主表（ior_posts）：存当前状态，快速查询
   - 版本表（ior_post_versions）：存所有历史，完整记录

### 优势

✅ 实现简单，开发效率高  
✅ 查询速度快，用户体验好  
✅ 数据完整性高，易于维护  
✅ 符合 Git 的使用习惯  

### 适用场景

- ✅ 博客文章、笔记、日志
- ✅ 文档管理系统
- ✅ 代码片段管理
- ✅ 任何需要版本控制的内容

---

**下一步**：运行 SQL 脚本创建表结构，然后启动项目测试接口！
