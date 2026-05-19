# 生命流模块 - CRUD 接口文档

## 📋 接口总览

### 帖子管理接口（/post）

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|---------|
| POST | `/post/create` | 创建帖子 | ✅ |
| PUT | `/post/{postId}` | 修改帖子（生成新版本） | ✅ |
| GET | `/post/{postId}` | 获取帖子详情 | ❌（私密需要） |
| DELETE | `/post/{postId}` | 删除帖子（软删除） | ✅ |
| GET | `/post/list` | 获取帖子列表（分页） | ❌（可选） |
| GET | `/post/my` | 获取我的帖子列表 | ✅ |
| GET | `/post/{postId}/versions` | 获取版本列表 | ✅ |
| GET | `/post/{postId}/versions/{versionId}` | 获取版本详情 | ✅ |
| POST | `/post/{postId}/versions/{versionId}/restore` | 恢复版本 | ✅ |

### 分类管理接口（/post/category）

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|---------|
| GET | `/post/category/list` | 获取分类列表 | ❌ |
| POST | `/post/category/create` | 创建分类 | ❌（应改为管理员） |
| PUT | `/post/category/{categoryId}` | 更新分类 | ❌（应改为管理员） |
| DELETE | `/post/category/{categoryId}` | 删除分类 | ❌（应改为管理员） |

---

## 🔧 详细接口说明

### 1. 创建帖子

**请求**：
```http
POST /post/create
Authorization: Bearer YOUR_TOKEN
Content-Type: application/x-www-form-urlencoded

title=我的第一篇生命流
&content=今天天气真好，心情不错
&visibility=PUBLIC
&tags=日常,心情
&categoryId=1
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 123  // 帖子ID
}
```

---

### 2. 修改帖子

**请求**：
```http
PUT /post/123
Authorization: Bearer YOUR_TOKEN
Content-Type: application/x-www-form-urlencoded

title=修改后的标题
&content=修改后的内容
&changeSummary=修正了错别字
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 2  // 新版本号
}
```

---

### 3. 获取帖子详情

**请求**：
```http
GET /post/123
Authorization: Bearer YOUR_TOKEN  // 私密帖子需要
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 123,
    "userId": 1,
    "title": "帖子标题",
    "content": "帖子内容",
    "visibility": "PUBLIC",
    "status": 1,
    "tags": "日常,心情",
    "categoryId": 1,
    "versionCount": 3,
    "currentVersionId": 303,
    "createdAt": "2026-05-19T10:00:00",
    "updatedAt": "2026-05-19T11:00:00"
  }
}
```

---

### 4. 删除帖子

**请求**：
```http
DELETE /post/123
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "删除成功"
}
```

---

### 5. 获取帖子列表

**请求**：
```http
GET /post/list?categoryId=1&tag=日常&page=1&size=20
Authorization: Bearer YOUR_TOKEN  // 可选，用于查看私密帖子
```

**参数说明**：
- `categoryId`: 分类ID（0或不传表示全部）
- `tag`: 标签（模糊匹配）
- `page`: 页码（默认1）
- `size`: 每页数量（默认20，最大100）

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "title": "帖子标题",
      "content": "帖子内容...",
      "userId": 1,
      "visibility": "PUBLIC",
      "tags": "日常,心情",
      "categoryId": 1,
      "versionCount": 3,
      "createdAt": "2026-05-19T10:00:00"
    },
    ...
  ]
}
```

---

### 6. 获取我的帖子列表

**请求**：
```http
GET /post/my?page=1&size=20
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "title": "我的私密帖子",
      "visibility": "PRIVATE",
      "status": 1,
      ...
    },
    ...
  ]
}
```

**特点**：
- ✅ 返回所有帖子（包括私密的）
- ✅ 返回已删除的帖子（status=0）
- ✅ 按创建时间倒序

---

### 7. 获取版本列表

**请求**：
```http
GET /post/123/versions
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 303,
      "postId": 123,
      "versionNumber": 3,
      "title": "第三版标题",
      "content": "第三版内容",
      "changeSummary": "增加了新内容",
      "createdBy": 1,
      "createdAt": "2026-05-19T12:00:00"
    },
    {
      "id": 302,
      "versionNumber": 2,
      "changeSummary": "修正了错别字",
      "createdAt": "2026-05-19T11:00:00"
    },
    {
      "id": 301,
      "versionNumber": 1,
      "changeSummary": "初始版本",
      "createdAt": "2026-05-19T10:00:00"
    }
  ]
}
```

---

### 8. 获取版本详情

**请求**：
```http
GET /post/123/versions/301
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 301,
    "postId": 123,
    "versionNumber": 1,
    "title": "第一版标题",
    "content": "第一版内容",
    "changeSummary": "初始版本",
    "createdBy": 1,
    "createdAt": "2026-05-19T10:00:00"
  }
}
```

---

### 9. 恢复版本

**请求**：
```http
POST /post/123/versions/301/restore
Authorization: Bearer YOUR_TOKEN
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 4  // 新生成的版本号
}
```

**说明**：
- 恢复操作会生成一个新版本（v4）
- v4 的内容与 v1 完全相同
- 版本历史保持线性完整

---

### 10. 获取分类列表

**请求**：
```http
GET /post/category/list
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "日常",
      "description": "日常生活记录",
      "parentId": 0,
      "sortOrder": 1,
      "isActive": 1
    },
    {
      "id": 2,
      "name": "工作",
      "description": "工作内容相关",
      "parentId": 0,
      "sortOrder": 2,
      "isActive": 1
    },
    ...
  ]
}
```

---

### 11. 创建分类

**请求**：
```http
POST /post/category/create
Content-Type: application/x-www-form-urlencoded

name=新技术
&description=技术学习笔记
&parentId=0
&sortOrder=7
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 7  // 分类ID
}
```

---

### 12. 更新分类

**请求**：
```http
PUT /post/category/7
Content-Type: application/x-www-form-urlencoded

name=技术开发
&sortOrder=6
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "更新成功"
}
```

---

### 13. 删除分类

**请求**：
```http
DELETE /post/category/7
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "删除成功"
}
```

**说明**：
- 软删除：将 `is_active` 设置为 0
- 分类不再出现在列表中
- 已有帖子的分类不受影响

---

## 🧪 测试流程建议

### 1. 初始化测试数据

```bash
# 1. 注册用户并登录，获取 Token
curl -X POST http://localhost:886/user/register \
  -d "email=test@example.com&password=Test@1234&code=123456"

curl -X POST http://localhost:886/user/login \
  -d "identifier=test@example.com&password=Test@1234"

# 保存返回的 Token
```

### 2. 测试分类管理

```bash
# 获取分类列表
curl http://localhost:886/post/category/list

# 创建新分类
curl -X POST http://localhost:886/post/category/create \
  -d "name=测试分类&description=用于测试&parentId=0&sortOrder=10"
```

### 3. 测试帖子 CRUD

```bash
# 创建帖子
curl -X POST http://localhost:886/post/create \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d "title=测试帖子&content=这是测试内容&visibility=PUBLIC&tags=测试&categoryId=1"

# 获取帖子列表
curl http://localhost:886/post/list?page=1&size=10

# 获取帖子详情
curl http://localhost:886/post/123

# 修改帖子
curl -X PUT http://localhost:886/post/123 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d "title=修改后的标题&content=修改后的内容&changeSummary=第一次修改"

# 获取版本列表
curl http://localhost:886/post/123/versions \
  -H "Authorization: Bearer YOUR_TOKEN"

# 删除帖子
curl -X DELETE http://localhost:886/post/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## ⚠️ 注意事项

### 1. 权限控制

- **公开接口**：获取帖子列表、获取帖子详情（公开）、获取分类列表
- **需要认证**：创建、修改、删除帖子，查看版本，查看私密帖子
- **管理员功能**：分类的增删改（目前未实现管理员权限校验）

### 2. 数据安全

- 私密帖子只有作者本人可见
- 版本历史只有作者本人可查看
- 删除帖子是软删除，数据仍保留在数据库中

### 3. 性能优化

- 帖子列表查询支持分页，避免一次性加载过多数据
- 建议后续集成 Redis 缓存（参考 `redis_cache_guide.md`）
- 标签筛选使用 `LIKE` 查询，大数据量时可能需要优化

### 4. 待完善功能

- ⏳ 媒体文件上传和管理（当前使用 JSON 存储 URL）
- ⏳ 管理员权限校验
- ⏳ 分类删除前检查是否有帖子引用
- ⏳ 全文搜索功能
- ⏳ 帖子点赞、收藏等互动功能

---

## 📊 接口统计

- **帖子管理**：9 个接口
- **分类管理**：4 个接口
- **总计**：13 个 REST API

所有接口均已集成 Knife4j 文档，访问 `http://localhost:886/doc.html` 即可查看和测试！
