# 生命流模块 - 前端开发接口文档 v2.0

> **最后更新**: 2026-05-19  
> **版本**: v2.0 (生产可用)  
> **基础URL**: `http://localhost:886`  
> **认证方式**: JWT Bearer Token

---

## 📋 目录

1. [快速开始](#快速开始)
2. [认证说明](#认证说明)
3. [文件上传接口](#文件上传接口)
4. [帖子管理接口](#帖子管理接口)
5. [版本管理接口](#版本管理接口)
6. [分类管理接口](#分类管理接口)
7. [数据结构说明](#数据结构说明)
8. [错误码说明](#错误码说明)
9. [前端集成示例](#前端集成示例)

---

## 🚀 快速开始

### 1. 注册并登录获取 Token

```javascript
// 1. 注册用户
POST /user/register
Content-Type: application/x-www-form-urlencoded

email=user@example.com&password=Test@1234&code=123456

// 2. 登录获取 Token
POST /user/login
Content-Type: application/x-www-form-urlencoded

identifier=user@example.com&password=Test@1234

// 响应
{
  "code": 200,
  "data": "eyJhbGciOiJIUzI1NiJ9..." // JWT Token
}
```

### 2. 在请求头中携带 Token

```javascript
// 所有需要认证的接口都需要在 Header 中添加
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 🔐 认证说明

### Token 使用规则

| 场景 | 是否需要 Token | 说明 |
|------|--------------|------|
| 公开帖子列表 | ❌ 可选 | 不传则只返回公开帖子 |
| 私密帖子详情 | ✅ 必需 | 只有作者可查看 |
| 创建/修改/删除 | ✅ 必需 | 验证作者权限 |
| 版本管理 | ✅ 必需 | 只有作者可操作 |
| 我的帖子/草稿 | ✅ 必需 | 查看个人数据 |

### Token 格式

```
Authorization: Bearer <token>
```

**注意**：
- Token 前必须加 `Bearer ` 前缀（有空格）
- Token 有效期：24小时
- Token 失效后需重新登录

---

## 📁 文件上传接口

### 1. 单文件上传

**接口**: `POST /file/upload`

**请求参数** (multipart/form-data):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | ✅ | 文件对象 |
| bizType | String | ✅ | 业务类型：`post`-帖子, `avatar`-头像 |

**支持的文件类型**:
- 图片: JPG, PNG, GIF, WEBP
- 视频: MP4, AVI, MOV
- 最大文件大小: 10MB

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "https://r2.example.com/posts/temp/images/abc123.jpg"
}
```

**前端调用示例**:

```javascript
async function uploadFile(file, bizType = 'post') {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('bizType', bizType);
  
  const response = await fetch('/file/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  const result = await response.json();
  return result.data; // 返回文件 URL
}
```

---

### 2. 批量上传

**接口**: `POST /file/upload/batch`

**请求参数** (multipart/form-data):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | File[] | ✅ | 文件数组（最多9个） |
| bizType | String | ✅ | 业务类型：`post`-帖子, `avatar`-头像 |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "https://r2.example.com/posts/temp/images/abc1.jpg",
    "https://r2.example.com/posts/temp/images/abc2.jpg",
    "https://r2.example.com/posts/temp/videos/abc3.mp4"
  ]
}
```

**前端调用示例**:

```javascript
async function uploadFiles(files, bizType = 'post') {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
  });
  formData.append('bizType', bizType);
  
  const response = await fetch('/file/upload/batch', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  const result = await response.json();
  return result.data; // 返回 URL 数组
}
```

---

## 📝 帖子管理接口

### 1. 创建帖子

**接口**: `POST /post/create`

**认证**: ✅ 必需

**请求参数** (application/x-www-form-urlencoded):

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| title | String | ✅ | - | 标题（最长200字符） |
| content | String | ✅ | - | 内容 |
| mediaUrls | String | ❌ | `[]` | 媒体URL数组（JSON字符串） |
| visibility | String | ❌ | `DRAFT` | 可见性：`PUBLIC`-公开, `PRIVATE`-私密, `DRAFT`-草稿 |
| tags | String | ❌ | `""` | 标签（逗号分隔，如：`日常,心情`） |
| categoryId | Long | ✅ | - | 分类ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 123  // 帖子ID
}
```

**前端调用示例**:

```javascript
async function createPost(postData) {
  const params = new URLSearchParams({
    title: postData.title,
    content: postData.content,
    mediaUrls: JSON.stringify(postData.mediaUrls || []),
    visibility: postData.visibility || 'DRAFT',
    tags: postData.tags || '',
    categoryId: postData.categoryId
  });
  
  const response = await fetch('/post/create', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: params
  });
  
  return await response.json();
}

// 使用示例
const result = await createPost({
  title: '我的第一篇生命流',
  content: '今天天气真好',
  mediaUrls: ['https://r2.example.com/posts/temp/images/abc.jpg'],
  visibility: 'PUBLIC',
  tags: '日常,心情',
  categoryId: 1
});
```

---

### 2. 修改帖子

**接口**: `PUT /post/{postId}`

**认证**: ✅ 必需（仅作者可修改）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |

**请求参数** (application/x-www-form-urlencoded):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | ✅ | 新标题 |
| content | String | ✅ | 新内容 |
| mediaUrls | String | ❌ | 新媒体URL数组（JSON字符串） |
| changeSummary | String | ❌ | 修改说明 |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 2  // 新版本号
}
```

**说明**:
- 每次修改会自动生成新版本
- 旧版本保留在历史中
- 可通过版本管理接口查看和恢复

---

### 3. 获取帖子详情

**接口**: `GET /post/{postId}`

**认证**: ❌ 可选（私密帖子需要Token且必须是作者）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "id": 123,
    "userId": 1,
    "title": "帖子标题",
    "content": "帖子内容",
    "mediaUrls": "[\"https://r2.example.com/image1.jpg\"]",
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

**接口**: `DELETE /post/{postId}`

**认证**: ✅ 必需（仅作者可删除）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "删除成功"
}
```

**说明**:
- 软删除：`status` 变为 0
- 数据仍保留在数据库中
- 可在"我的帖子"中查看已删除的帖子

---

### 5. 获取公共帖子列表

**接口**: `GET /post/list`

**认证**: ❌ 可选（传入Token可查看自己发布的私密帖子）

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| categoryId | Long | ❌ | 全部 | 分类ID（0或不传表示全部） |
| tag | String | ❌ | 全部 | 标签（模糊匹配） |
| page | Integer | ❌ | 1 | 页码 |
| size | Integer | ❌ | 20 | 每页数量（最大100） |

**响应示例**:

```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "userId": 1,
      "title": "帖子标题",
      "content": "帖子内容...",
      "mediaUrls": "[\"https://r2.example.com/image1.jpg\"]",
      "visibility": "PUBLIC",
      "status": 1,
      "tags": "日常,心情",
      "categoryId": 1,
      "versionCount": 3,
      "createdAt": "2026-05-19T10:00:00",
      "updatedAt": "2026-05-19T11:00:00"
    }
  ]
}
```

**重要说明**:
- ✅ 自动过滤草稿（`visibility=DRAFT`）
- ✅ 只返回正常状态的帖子（`status=1`）
- ✅ 按创建时间倒序排列

---

### 6. 获取我的帖子列表

**接口**: `GET /post/my/posts`

**认证**: ✅ 必需

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | ❌ | 1 | 页码 |
| size | Integer | ❌ | 20 | 每页数量（最大100） |

**响应示例**:

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
    {
      "id": 124,
      "title": "草稿",
      "visibility": "DRAFT",
      "status": 1,
      ...
    }
  ]
}
```

**特点**:
- ✅ 返回所有帖子（包括草稿、私密、已删除）
- ✅ 按更新时间倒序排列

---

### 7. 获取我的草稿列表

**接口**: `GET /post/my/drafts`

**认证**: ✅ 必需

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | ❌ | 1 | 页码 |
| size | Integer | ❌ | 20 | 每页数量（最大100） |

**响应示例**:

```json
{
  "code": 200,
  "data": [
    {
      "id": 124,
      "title": "草稿标题",
      "content": "草稿内容",
      "visibility": "DRAFT",
      "status": 1,
      "updatedAt": "2026-05-19T17:42:03"
    }
  ]
}
```

---

### 8. 发布草稿

**接口**: `POST /post/{postId}/publish`

**认证**: ✅ 必需（仅作者可发布）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |

**请求参数** (application/x-www-form-urlencoded):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| visibility | String | ✅ | 发布后的可见性：`PUBLIC` 或 `PRIVATE` |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "发布成功"
}
```

**前端调用示例**:

```javascript
async function publishDraft(postId, visibility = 'PUBLIC') {
  const response = await fetch(`/post/${postId}/publish?visibility=${visibility}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return await response.json();
}
```

---

## 🔄 版本管理接口

### 1. 获取版本列表

**接口**: `GET /post/{postId}/versions`

**认证**: ✅ 必需（仅作者可查看）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |

**响应示例**:

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
      "mediaUrls": "[\"https://r2.example.com/image.jpg\"]",
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

**说明**:
- 按版本号倒序排列（最新的在前）
- 只有作者可以查看版本历史

---

### 2. 获取版本详情

**接口**: `GET /post/{postId}/versions/{versionId}`

**认证**: ✅ 必需（仅作者可查看）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |
| versionId | Long | 版本ID |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "id": 301,
    "postId": 123,
    "versionNumber": 1,
    "title": "第一版标题",
    "content": "第一版内容",
    "mediaUrls": "[\"https://r2.example.com/image.jpg\"]",
    "changeSummary": "初始版本",
    "createdBy": 1,
    "createdAt": "2026-05-19T10:00:00"
  }
}
```

---

### 3. 恢复版本

**接口**: `POST /post/{postId}/versions/{versionId}/restore`

**认证**: ✅ 必需（仅作者可恢复）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| postId | Long | 帖子ID |
| versionId | Long | 要恢复的版本ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 4  // 新生成的版本号
}
```

**说明**:
- 恢复操作会生成一个新版本（例如 v4）
- 新版本的内容与目标版本完全相同
- 版本历史保持线性完整（不会覆盖旧版本）

**前端调用示例**:

```javascript
async function restoreVersion(postId, versionId) {
  const response = await fetch(`/post/${postId}/versions/${versionId}/restore`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return await response.json();
}
```

---

## 📂 分类管理接口

### 1. 获取分类列表

**接口**: `GET /post/category/list`

**认证**: ❌ 不需要

**响应示例**:

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
    }
  ]
}
```

---

### 2. 创建分类

**接口**: `POST /post/category/create`

**认证**: ⚠️ 建议添加管理员权限校验

**请求参数** (application/x-www-form-urlencoded):

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| name | String | ✅ | - | 分类名称（最长50字符） |
| description | String | ❌ | `""` | 分类描述 |
| parentId | Long | ❌ | 0 | 父分类ID（0表示一级分类） |
| sortOrder | Integer | ❌ | 0 | 排序权重（越小越靠前） |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 7  // 分类ID
}
```

---

### 3. 更新分类

**接口**: `PUT /post/category/{categoryId}`

**认证**: ⚠️ 建议添加管理员权限校验

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| categoryId | Long | 分类ID |

**请求参数** (application/x-www-form-urlencoded):

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | ❌ | 新名称 |
| description | String | ❌ | 新描述 |
| sortOrder | Integer | ❌ | 新排序权重 |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "更新成功"
}
```

---

### 4. 删除分类

**接口**: `DELETE /post/category/{categoryId}`

**认证**: ⚠️ 建议添加管理员权限校验

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| categoryId | Long | 分类ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "删除成功"
}
```

**说明**:
- 软删除：`is_active` 设置为 0
- 分类不再出现在列表中
- 已有帖子的分类不受影响

---

## 📊 数据结构说明

### 帖子对象 (IorPost)

```typescript
interface Post {
  id: number;              // 帖子ID
  userId: number;          // 作者ID
  parentId: number;        // 父帖子ID（0表示根帖子）
  title: string;           // 标题
  content: string;         // 内容
  mediaUrls: string;       // 媒体URL数组（JSON字符串）
  visibility: string;      // 可见性：PUBLIC/PRIVATE/DRAFT
  status: number;          // 状态：0-删除, 1-正常
  tags: string;            // 标签（逗号分隔）
  categoryId: number;      // 分类ID
  versionCount: number;    // 总版本数量
  currentVersionId: number;// 当前版本ID
  deletedAt: string;       // 删除时间
  createdAt: string;       // 创建时间
  updatedAt: string;       // 更新时间
}
```

### 版本对象 (IorPostVersion)

```typescript
interface PostVersion {
  id: number;              // 版本ID
  postId: number;          // 帖子ID
  versionNumber: number;   // 版本号（从1开始）
  title: string;           // 标题快照
  content: string;         // 内容快照
  mediaUrls: string;       // 媒体URL快照（JSON字符串）
  changeSummary: string;   // 修改说明
  createdBy: number;       // 修改者ID
  createdAt: string;       // 版本创建时间
}
```

### 分类对象 (IorPostCategory)

```typescript
interface Category {
  id: number;              // 分类ID
  name: string;            // 分类名称
  description: string;     // 分类描述
  parentId: number;        // 父分类ID（0表示一级分类）
  sortOrder: number;       // 排序权重
  isActive: number;        // 是否启用：0-禁用, 1-启用
}
```

### 媒体URL解析

`mediaUrls` 字段是 **JSON 字符串**，需要解析：

```javascript
// 后端返回
const mediaUrlsString = '["https://r2.example.com/image1.jpg", "https://r2.example.com/video1.mp4"]';

// 前端解析
const mediaUrls = JSON.parse(mediaUrlsString);
// 结果: ["https://r2.example.com/image1.jpg", "https://r2.example.com/video1.mp4"]

// 提交时转回字符串
const mediaUrlsString = JSON.stringify(mediaUrls);
```

---

## ❌ 错误码说明

| 错误码 | 说明 | 常见原因 |
|--------|------|---------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 参数缺失、格式错误、验证码错误 |
| 401 | 未认证 | Token 缺失或无效 |
| 403 | 无权限 | 非作者尝试修改/删除/查看私密帖子 |
| 404 | 资源不存在 | 帖子ID、版本ID、分类ID不存在 |
| 500 | 服务器内部错误 | 数据库错误、R2上传失败等 |

**错误响应格式**:

```json
{
  "code": 400,
  "message": "验证码错误或已过期",
  "data": null
}
```

---

## 💻 前端集成示例

### Vue 3 + TypeScript 示例

```typescript
// api/post.ts
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:886',
  timeout: 10000
});

// 请求拦截器：自动添加 Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 获取公共帖子列表
export async function getPublicPosts(params: {
  categoryId?: number;
  tag?: string;
  page?: number;
  size?: number;
}) {
  const response = await api.get('/post/list', { params });
  return response.data;
}

// 创建帖子
export async function createPost(data: {
  title: string;
  content: string;
  mediaUrls?: string[];
  visibility?: string;
  tags?: string;
  categoryId: number;
}) {
  const params = new URLSearchParams({
    title: data.title,
    content: data.content,
    mediaUrls: JSON.stringify(data.mediaUrls || []),
    visibility: data.visibility || 'DRAFT',
    tags: data.tags || '',
    categoryId: String(data.categoryId)
  });
  
  const response = await api.post('/post/create', params);
  return response.data;
}

// 上传文件
export async function uploadFile(file: File, bizType: string = 'post') {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('bizType', bizType);
  
  const response = await api.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  
  return response.data;
}
```

### React 示例

```jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

function PostList() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchPosts();
  }, []);

  const fetchPosts = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/post/list', {
        params: { page: 1, size: 20 }
      });
      setPosts(response.data.data);
    } catch (error) {
      console.error('获取帖子列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>加载中...</div>;

  return (
    <div>
      {posts.map(post => (
        <div key={post.id}>
          <h3>{post.title}</h3>
          <p>{post.content.substring(0, 100)}...</p>
          <span>{post.tags}</span>
        </div>
      ))}
    </div>
  );
}
```

---

## 🧪 测试流程

### 完整业务流程测试

```bash
# 1. 注册并登录
curl -X POST http://localhost:886/user/register \
  -d "email=test@example.com&password=Test@1234&code=123456"

TOKEN=$(curl -X POST http://localhost:886/user/login \
  -d "identifier=test@example.com&password=Test@1234" | jq -r '.data')

# 2. 上传文件
FILE_URL=$(curl -X POST http://localhost:886/file/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.jpg" \
  -F "bizType=post" | jq -r '.data')

# 3. 创建帖子（草稿）
POST_ID=$(curl -X POST http://localhost:886/post/create \
  -H "Authorization: Bearer $TOKEN" \
  -d "title=测试帖子" \
  -d "content=这是测试内容" \
  -d "mediaUrls=[\"$FILE_URL\"]" \
  -d "visibility=DRAFT" \
  -d "tags=测试" \
  -d "categoryId=1" | jq -r '.data')

# 4. 查看草稿
curl http://localhost:886/post/my/drafts \
  -H "Authorization: Bearer $TOKEN"

# 5. 发布草稿
curl -X POST http://localhost:886/post/$POST_ID/publish?visibility=PUBLIC \
  -H "Authorization: Bearer $TOKEN"

# 6. 查看公共列表
curl http://localhost:886/post/list?page=1&size=10

# 7. 修改帖子
curl -X PUT http://localhost:886/post/$POST_ID \
  -H "Authorization: Bearer $TOKEN" \
  -d "title=修改后的标题" \
  -d "content=修改后的内容" \
  -d "changeSummary=第一次修改"

# 8. 查看版本历史
curl http://localhost:886/post/$POST_ID/versions \
  -H "Authorization: Bearer $TOKEN"

# 9. 恢复到第一个版本
VERSION_ID=$(curl http://localhost:886/post/$POST_ID/versions \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data[-1].id')

curl -X POST http://localhost:886/post/$POST_ID/versions/$VERSION_ID/restore \
  -H "Authorization: Bearer $TOKEN"

# 10. 删除帖子
curl -X DELETE http://localhost:886/post/$POST_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## ⚠️ 注意事项

### 1. 跨域配置

如果前端和后端不在同一域名，需要配置 CORS：

```yaml
# application.yaml
spring:
  web:
    cors:
      allowed-origins: http://localhost:3000,http://localhost:5173
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
```

### 2. 文件大小限制

Spring Boot 默认限制：
- 单个文件最大：1MB
- 请求总大小：10MB

如需调整：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB
```

### 3. Token 刷新

Token 有效期为 24 小时，建议：
- 前端检测 Token 过期时间
- 过期前自动刷新或提示用户重新登录
- 将 Token 存储在 `localStorage` 或 `sessionStorage`

### 4. 错误处理

建议统一处理错误：

```javascript
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Token 失效，跳转登录页
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      // 无权限，提示用户
      alert('您没有权限执行此操作');
    }
    return Promise.reject(error);
  }
);
```

---

## 📞 技术支持

- **Knife4j 文档**: http://localhost:886/doc.html
- **GitHub**: [项目地址]
- **问题反馈**: [Issues 地址]

---

**文档版本**: v2.0  
**最后更新**: 2026-05-19  
**维护者**: IOR Backend Team
