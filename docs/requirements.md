# IOR 项目需求文档

## 版本历史
| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| v1.0 | 2026-05-18 | - | 初始版本:用户模块基础功能 |
| v1.1 | 2026-05-19 | - | 完善用户模块细节,新增生命流模块详细设计 |

---

## 一、用户模块

### 1.1 用户注册
**需求描述**:支持用户通过邮箱注册账号

**功能要点**:
- 注册方式:邮箱注册
- 邮件服务:使用 QQ SMTP 服务发送验证码
- 验证码机制:
  - 验证码类型:register(注册)、login(登录)、reset_password(重置密码)、change_email(修改邮箱)
  - 类型隔离:不同类型的验证码不能混用
  - 有效期:5分钟
  - 错误尝试限制:最多尝试5次,超过后需重新获取
  - 防重复发送:同一邮箱60秒内只能发送一次
- 唯一性校验:账号(account)、用户名(username)、邮箱(email)必须全局唯一
- 密码强度要求:
  - 长度:8-20位
  - 必须包含:大写字母、小写字母、数字、特殊字符
  - 禁止连续3个相同字符
  - 禁止常见弱密码(password、admin123等)

**验收标准**:
- ✅ 邮箱格式验证正确
- ✅ 验证码发送成功且类型隔离生效
- ✅ 密码强度校验生效
- ✅ 重复账号/用户名/邮箱注册被拒绝
- ✅ 注册成功后可正常登录

---

### 1.2 用户登录
**需求描述**:支持多种方式和标识符登录系统

**功能要点**:
- 支持的标识符:账号(account)/ 用户名(username)/ 邮箱(email)
- 登录方式:
  1. **密码登录**:标识符 + 密码
  2. **验证码登录**:邮箱 + 验证码(使用 login 类型验证码)
- JWT Token 机制:
  - Access Token:有效期1小时,用于接口鉴权
  - Token 携带信息:userId、role
  - Token 刷新:暂不支持,过期后需重新登录
- 角色权限体系:
  - USER:普通用户(默认)
  - VIP:会员用户
  - ADMIN:管理员
- 安全策略:
  - 登录失败限制:同一账号连续失败5次,锁定15分钟
  - 多设备登录:允许多设备同时登录
  - 登录日志:记录登录时间、IP地址(待实现真实IP获取)
- 冷静期撤销:处于注销冷静期的用户登录时,自动撤销注销申请

**验收标准**:
- ✅ 三种标识符均可成功登录
- ✅ 密码登录和验证码登录均正常工作
- ✅ JWT Token 生成和验证正确
- ✅ 登录失败次数限制生效
- ✅ 冷静期用户登录后状态恢复正常

---

### 1.3 用户修改基本身份信息
**需求描述**:用户可以修改部分个人信息,敏感操作需要额外验证

**功能要点**:
- **不可修改字段**:
  - 账号(account):永久不可修改
  - 用户名(username):永久不可修改
  
- **可直接修改**(无需额外验证):
  - 昵称(nickname):随时修改
  - 头像(avatar):上传新头像(集成 Cloudflare R2 存储)
  
- **需要验证的修改**:
  1. **修改密码**:
     - 需验证旧密码
     - 新密码需符合强度要求
     - 修改成功后原 Token 立即失效(加入黑名单)
     - 用户需重新登录
  
  2. **修改邮箱**:
     - 需验证旧邮箱验证码(change_email 类型)
     - 需验证新邮箱验证码(change_email 类型)
     - 新邮箱不能被其他账号占用
     - 修改成功后原 Token 立即失效
     - 用户需重新登录

**验收标准**:
- ✅ 账号和用户名无法修改
- ✅ 昵称和头像可以正常修改
- ✅ 修改密码时旧密码验证正确
- ✅ 修改邮箱时双重验证生效
- ✅ 敏感操作后 Token 失效,需重新登录

---

### 1.4 用户注销
**需求描述**:提供安全的账户注销机制,包含冷静期和自动删除

**功能要点**:
- **注销流程**:
  1. 身份验证:发送验证码到绑定邮箱(使用 reset_password 类型)
  2. 用户确认:提交注销申请,可选填写注销原因
  3. 进入冷静期:用户状态变更为"待注销"(status=2)
  4. 冷静期时长:7天
  
- **撤销机制**:
  - 触发条件:冷静期内用户任意一次登录
  - 自动操作:
    - 注销申请状态改为 CANCELLED
    - 用户状态恢复为正常(status=1)
    - 记录取消时间
  
- **到期处理**:
  - 定时任务:每天凌晨2点执行
  - 扫描条件:completed_at < 当前时间 且 status = PENDING
  - 执行操作:
    - 根据用户角色选择对应策略(策略模式)
    - 执行逻辑删除(deleted_at 设置为当前时间)
    - 注销申请状态改为 COMPLETED
  
- **数据保留**:
  - 采用逻辑删除,不物理删除数据
  - 便于数据审计和可能的恢复需求

**验收标准**:
- ✅ 注销申请需要验证码验证
- ✅ 冷静期内登录自动撤销注销
- ✅ 7天后自动执行逻辑删除
- ✅ 定时任务正常执行
- ✅ 已删除用户无法登录

---

## 二、帖子/日志/生命流模块

### 2.1 核心概念定义

**生命流(Life Stream)**:
- 用户创建的内容单元,记录生活、想法、经历等
- 支持文本、图片、视频等多种媒体形式
- 具有版本管理能力,类似 Git 的版本控制
- 可形成树状关联结构

**关键特性**:
- 版本管理:每次修改生成新版本,保留历史记录
- 可见性控制:公开/私密两种模式
- 关联扩展:可在帖子下添加关联帖子,形成讨论链
- 标签分类:支持标签和分类管理

---

### 2.2 帖子创建

**需求描述**:用户可以创建新的生命流帖子

**功能要点**:
- **内容类型**:
  - 标题(必填):最多200字符
  - 正文(必填):文本内容,支持富文本(后期扩展)
  - 图片(可选):
    - 最多9张
    - 单张不超过10MB
    - 支持格式:JPG、PNG、GIF、WEBP、BMP
  - 视频(可选):
    - 最多1个
    - 不超过500MB
    - 支持格式:MP4、AVI、MOV
  - 混合内容:文本 + 图片/视频

- **可见性设置**:
  - 公开(PUBLIC):所有人可见(默认)
  - 私密(PRIVATE):仅自己可见

- **标签系统**:
  - 每个帖子最多添加10个标签
  - 单个标签长度:1-20个字符
  - 标签仅支持字母、数字、中文、下划线
  - 热门标签推荐(后期扩展)

- **分类归属**:
  - 每个帖子必须选择一个分类
  - 分类由管理员预设
  - 支持层级分类(一级、二级)

- **存储方式**:
  - 文本内容:数据库 TEXT 字段
  - 媒体文件:上传至 Cloudflare R2,数据库存储 URL
  - 标签:逗号分隔字符串(初期),后期可独立建表

**验收标准**:
- ✅ 可以创建纯文本帖子
- ✅ 可以上传图片和视频
- ✅ 文件大小和格式限制生效
- ✅ 可见性设置正确保存
- ✅ 标签和分类正确关联

---

### 2.3 帖子修改与版本管理

**需求描述**:用户可以修改自己的帖子,系统自动进行版本管理

**功能要点**:
- **版本触发条件**:
  - 触发生成新版本:
    - 修改标题
    - 修改正文内容
    - 修改图片/视频文件
  - 不触发新版本:
    - 仅修改标签
    - 仅修改可见性
    - 仅修改分类

- **版本信息记录**:
  - 版本号:从1开始递增(v1, v2, v3...)
  - 修改时间:精确到秒
  - 修改人:用户ID
  - 修改说明:用户可选填写(最多500字符)
  - 内容快照:完整保存修改前的标题、内容、媒体URL

- **版本查看**:
  - 版本列表:
    - 按时间倒序展示
    - 显示版本号、修改时间、修改说明
    - 标注当前版本
  - 版本详情:
    - 查看某个版本的完整内容
    - 显示该版本的所有媒体文件
  - 版本对比(高级功能,后期实现):
    - 高亮显示两个版本的差异
    - 文本差异对比
    - 媒体文件变化提示

- **版本恢复**:
  - 可恢复到任意历史版本
  - 恢复操作本身也生成新版本
  - 例如:当前v5,恢复到v2,生成v6(内容与v2相同)

- **存储策略**:
  - 采用快照方式:保存完整内容副本
  - 优点:查询快、实现简单、易于恢复
  - 缺点:存储空间较大(可通过压缩优化)

**验收标准**:
- ✅ 修改内容自动生成新版本
- ✅ 版本列表正确显示所有历史版本
- ✅ 可以查看任意版本的详细内容
- ✅ 版本恢复功能正常工作
- ✅ 仅修改标签不生成新版本

---

### 2.4 帖子删除

**需求描述**:用户可以删除自己的帖子,支持软删除和恢复

**功能要点**:
- **删除机制**:
  - 软删除:标记删除状态,不物理删除
  - 删除字段:status = 0(已删除)
  - 删除时间:记录 deleted_at

- **删除范围**:
  - 删除根帖子:
    - 所有子帖子一并隐藏
    - 子帖子不实际删除,仅不可见
  - 删除子帖子:
    - 仅隐藏该节点
    - 不影响其他子帖子

- **恢复机制**:
  - 删除后30天内可恢复
  - 恢复后状态改回正常(status = 1)
  - 超过30天不可恢复(需联系管理员)

- **权限控制**:
  - 仅作者和管理员可删除
  - 普通用户不能删除他人帖子

**验收标准**:
- ✅ 删除后帖子不再出现在列表中
- ✅ 删除根帖子时子帖子同时隐藏
- ✅ 30天内可以恢复
- ✅ 权限控制正确

---

### 2.5 关联扩展(类评论系统)

**需求描述**:在原有帖子下新增关联帖子,形成树状结构

**功能要点**:
- **树状结构设计**:
  - 根帖子:parent_id = 0
  - 子帖子:parent_id = 父帖子ID
  - 最大深度:限制为5层(避免过深嵌套)

- **与普通评论的区别**:
  - 独立性:子帖子是独立的内容单元
  - 版本管理:子帖子有自己的版本历史
  - 可见性:子帖子可独立设置公开/私密
  - 可分享:子帖子可以被单独分享和访问
  - 媒体支持:子帖子同样支持图片、视频

- **展示方式**:
  - 时间线模式:按发布时间排序(默认)
  - 树状模式:按层级关系展示(可选)
  - 热度模式:按回复数量排序(后期扩展)

- **查询优化**:
  - 递归查询:使用 CTE(公用表表达式)查询子树
  - 分页策略:每层单独分页,避免一次性加载过多

**验收标准**:
- ✅ 可以创建子帖子
- ✅ 树状结构正确建立
- ✅ 可以查询帖子的所有子帖子
- ✅ 最大深度限制生效
- ✅ 子帖子独立管理版本和可见性

---

### 2.6 标签与分类

**需求描述**:提供帖子的组织和管理机制

**功能要点**:
- **标签(Tags)**:
  - 创建方式:用户自由创建
  - 数量限制:每个帖子最多10个标签
  - 格式要求:1-20字符,仅允许字母、数字、中文、下划线
  - 搜索功能:支持按标签搜索帖子
  - 热门统计:统计热门标签(后期扩展)
  - 存储方式:逗号分隔字符串(初期)

- **分类(Categories)**:
  - 管理方式:管理员预设和维护
  - 层级结构:支持一级分类和二级分类
  - 必选属性:每个帖子必须属于一个分类
  - 分类示例:日常、工作、学习、旅行、思考、创作等
  - 分类管理接口:增删改查(管理员专用)

**验收标准**:
- ✅ 可以为帖子添加和修改标签
- ✅ 可以按标签搜索帖子
- ✅ 分类列表正确显示
- ✅ 发帖时必须选择分类
- ✅ 管理员可以管理分类

---

### 2.7 帖子查询与展示

**需求描述**:提供灵活的帖子查询和展示功能

**功能要点**:
- **分页查询**:
  - 支持页码和每页数量参数
  - 默认每页20条
  - 最大每页100条
  - 使用游标分页(后期优化,避免深分页性能问题)

- **筛选条件**:
  - 按作者:user_id
  - 按分类:category_id
  - 按标签:tag(模糊匹配)
  - 按时间范围:start_time ~ end_time
  - 按可见性:public/private
  - 按状态:正常/已删除(管理员)

- **排序方式**:
  - 最新发布:created_at DESC(默认)
  - 最新修改:updated_at DESC
  - 最多关联:子帖子数量 DESC(热度)
  - 最早发布:created_at ASC

- **搜索功能**(后期扩展):
  - 全文搜索:标题 + 内容
  - 标签搜索:精确匹配
  - 作者搜索:按用户名或账号
  - 搜索引擎:集成 Elasticsearch(可选)

- **权限过滤**:
  - 公开帖子:所有人可见
  - 私密帖子:仅作者本人可见
  - 管理员:可见所有帖子

**验收标准**:
- ✅ 分页查询正确返回结果
- ✅ 筛选条件组合查询正常
- ✅ 排序功能正常工作
- ✅ 权限过滤正确(私密帖子不泄露)
- ✅ 查询性能符合要求(响应时间 < 500ms)

---

### 2.8 权限控制矩阵

| 操作 | 作者本人 | 其他用户 | 管理员 |
|------|---------|---------|--------|
| 查看公开帖子 | ✅ | ✅ | ✅ |
| 查看私密帖子 | ✅ | ❌ | ✅ |
| 创建帖子 | ✅ | ❌ | ✅ |
| 修改自己的帖子 | ✅ | ❌ | ✅ |
| 删除自己的帖子 | ✅ | ❌ | ✅ |
| 查看版本历史 | ✅ | ❌ | ✅ |
| 恢复历史版本 | ✅ | ❌ | ✅ |
| 查看已删除帖子 | ✅ | ❌ | ✅ |
| 恢复已删除帖子 | ✅ (30天内) | ❌ | ✅ |

---

## 三、数据库设计

### 3.1 帖子主表(ior_posts)
```sql
CREATE TABLE ior_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父帖子ID(0表示根帖子)',
    title VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题',
    content TEXT COMMENT '文本内容',
    media_urls JSON COMMENT '媒体文件URL数组',
    visibility ENUM('PUBLIC', 'PRIVATE') DEFAULT 'PUBLIC' COMMENT '可见性',
    status TINYINT DEFAULT 1 COMMENT '状态:0-删除, 1-正常',
    tags VARCHAR(500) DEFAULT '' COMMENT '标签(逗号分隔)',
    category_id BIGINT DEFAULT 0 COMMENT '分类ID',
    version_count INT DEFAULT 1 COMMENT '版本数量',
    current_version_id BIGINT COMMENT '当前版本ID',
    deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '删除时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_visibility (visibility),
    INDEX idx_category_id (category_id),
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子主表';
```

### 3.2 帖子版本表(ior_post_versions)
```sql
CREATE TABLE ior_post_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    version_number INT NOT NULL COMMENT '版本号',
    title VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题快照',
    content TEXT COMMENT '内容快照',
    media_urls JSON COMMENT '媒体文件URL快照',
    change_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '修改说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
    created_by BIGINT NOT NULL COMMENT '修改者ID',
    INDEX idx_post_id (post_id),
    INDEX idx_post_version (post_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子版本历史表';
```

### 3.3 帖子分类表(ior_post_categories)
```sql
CREATE TABLE ior_post_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) NOT NULL DEFAULT '' COMMENT '分类描述',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID(0表示一级分类)',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用:0-禁用, 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子分类表';
```

---

## 四、接口设计(生命流模块)

### 4.1 创建帖子
- **URL**: `POST /post/create`
- **认证**: 需要 JWT Token
- **请求参数**:
```json
{
  "title": "我的第一篇生命流",
  "content": "今天天气真好...",
  "mediaFiles": ["file1", "file2"],
  "visibility": "PUBLIC",
  "tags": ["日常", "心情"],
  "categoryId": 1
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": { "postId": 123 }
}
```

### 4.2 获取帖子列表
- **URL**: `GET /post/list`
- **认证**: 公开接口(私密帖子需要Token)
- **查询参数**: page, pageSize, categoryId, tag, userId, visibility, sortBy
- **响应**:
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 123,
        "title": "帖子标题",
        "author": "作者名",
        "createdAt": "2026-05-19T10:00:00",
        "versionCount": 3
      }
    ]
  }
}
```

### 4.3 获取帖子详情
- **URL**: `GET /post/{postId}`
- **认证**: 公开接口(私密帖子需要作者或管理员权限)
- **响应**: 包含当前版本的完整内容和媒体URL

### 4.4 修改帖子
- **URL**: `PUT /post/{postId}`
- **认证**: 需要 JWT Token(仅作者或管理员)
- **请求参数**: 同创建帖子,额外增加 changeSummary 字段
- **响应**: 生成新版本,返回新的版本号

### 4.5 删除帖子
- **URL**: `DELETE /post/{postId}`
- **认证**: 需要 JWT Token(仅作者或管理员)
- **响应**: 软删除成功

### 4.6 获取版本列表
- **URL**: `GET /post/{postId}/versions`
- **认证**: 需要 JWT Token(仅作者或管理员)
- **响应**: 版本列表(版本号、修改时间、修改说明)

### 4.7 获取版本详情
- **URL**: `GET /post/{postId}/versions/{versionId}`
- **认证**: 需要 JWT Token(仅作者或管理员)
- **响应**: 该版本的完整内容

### 4.8 恢复版本
- **URL**: `POST /post/{postId}/versions/{versionId}/restore`
- **认证**: 需要 JWT Token(仅作者或管理员)
- **响应**: 生成新版本,内容与指定版本相同

---

## 五、非功能性需求

### 5.1 性能要求
- 接口响应时间:95% 的请求 < 500ms
- 帖子列表查询:< 300ms
- 文件上传:< 5s(取决于文件大小和网络)
- 并发支持:至少 1000 QPS

### 5.2 安全要求
- 所有敏感操作需要 JWT 认证
- 密码使用 BCrypt 加密存储
- 防止 XSS 攻击:帖子内容需要转义或白名单过滤
- 防止 SQL 注入:使用 MyBatis-Plus 参数化查询
- 文件上传安全:验证文件真实类型,限制文件大小
- 频率限制:验证码发送、登录等操作需要限流

### 5.3 可用性要求
- 系统可用性:99.9%
- 数据持久化:重要数据实时同步
- 备份策略:每日全量备份,每小时增量备份

### 5.4 可扩展性
- 微服务架构预留:模块间松耦合
- 数据库读写分离:主从复制(后期)
- 缓存策略:Redis 缓存热点数据
- CDN 加速:静态资源使用 CDN

---

## 六、开发计划

### 第一阶段(P0 - 核心功能)
- ✅ 用户注册、登录、注销
- ✅ 退出登录功能
- ✅ 密码重置功能
- ✅ 获取用户信息
- 🔄 帖子创建(文本+图片)
- 🔄 帖子列表/详情查询
- 🔄 帖子修改 + 版本管理基础版

### 第二阶段(P1 - 重要功能)
- ⏳ 帖子删除(软删除)
- ⏳ 可见性控制(公开/私密)
- ⏳ 标签系统
- ⏳ 分类系统
- ⏳ 登录日志记录

### 第三阶段(P2 - 增强功能)
- ⏳ 关联帖子(树状结构)
- ⏳ 版本对比(diff展示)
- ⏳ 全文搜索
- ⏳ 视频上传支持
- ⏳ 手动撤销注销接口

### 第四阶段(P3 - 扩展功能)
- ⏳ 点赞/收藏/分享
- ⏳ 数据统计看板
- ⏳ 内容审核系统
- ⏳ 消息通知
- ⏳ 管理员后台

---

## 七、技术栈

### 后端技术
- **核心框架**: Spring Boot 3.5.14
- **编程语言**: Java 17
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis-Plus 3.5.9
- **缓存**: Redis (Lettuce)
- **安全认证**: Spring Security + JWT (jjwt)
- **接口文档**: Knife4j (OpenAPI 3)
- **文件存储**: Cloudflare R2 (AWS S3 SDK)
- **邮件服务**: Spring Mail (QQ SMTP)
- **工具库**: Hutool, Lombok
- **构建工具**: Maven 3.9+

### 开发工具
- **IDE**: IntelliJ IDEA
- **版本控制**: Git
- **接口测试**: Knife4j UI / Postman
- **数据库管理**: Navicat / DBeaver

---

## 八、注意事项

### 8.1 开发规范
- 遵循 RESTful API 设计规范
- 统一响应格式:Result<T>
- 异常处理:全局异常处理器
- 日志记录:关键操作必须记录日志
- 代码注释:公共方法必须有 Javadoc

### 8.2 安全注意事项
- 严禁在代码中硬编码敏感信息(使用 .env 文件)
- .env 文件已加入 .gitignore,不会提交到版本库
- 生产环境必须使用 HTTPS
- 定期更新依赖包版本,修复安全漏洞

### 8.3 性能优化建议
- 数据库查询避免 N+1 问题
- 使用索引优化查询性能
- 热点数据缓存到 Redis
- 大文件分片上传(后期优化)
- 图片压缩和缩略图生成

### 8.4 部署建议
- 使用 Docker 容器化部署
- 配置环境变量管理不同环境
- 使用 Nginx 反向代理
- 配置日志轮转和监控告警

