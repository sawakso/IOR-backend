# 数据库字段规范文档

## 一、命名规范

### 1.1 表名规范
- **前缀**：所有表名使用 `ior_` 作为前缀，便于区分不同模块
- **命名风格**：使用小写字母 + 下划线（snake_case）
- **示例**：
  - ✅ `ior_users`
  - ✅ `ior_verification_codes`
  - ❌ `Users`
  - ❌ `iorUsers`
  - ❌ `IOR_USERS`

### 1.2 字段名规范
- **命名风格**：使用小写字母 + 下划线（snake_case）
- **语义清晰**：字段名应清晰表达含义
- **避免保留字**：不使用 MySQL 保留字作为字段名
- **示例**：
  - ✅ `user_id`
  - ✅ `created_at`
  - ✅ `password_hash`
  - ❌ `userId`（驼峰命名）
  - ❌ `User_ID`（混合大小写）
  - ❌ `desc`（MySQL 保留字）

### 1.3 索引命名规范
- **普通索引**：`idx_字段名` 或 `idx_字段1_字段2`
- **唯一索引**：`uk_字段名` 或 `uk_字段1_字段2`
- **外键索引**：`fk_表名_字段名`
- **示例**：
  - ✅ `idx_email`
  - ✅ `idx_user_id_created_at`
  - ✅ `uk_username`
  - ✅ `fk_orders_user_id`

---

## 二、数据类型规范

### 2.1 主键 ID
```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID'
```
- **类型**：`BIGINT`（不用 INT，避免数据量大时溢出）
- **自增**：`AUTO_INCREMENT`
- **说明**：统一使用 BIGINT，为未来数据增长预留空间

### 2.2 字符串类型

#### VARCHAR vs CHAR
- **VARCHAR**：变长字符串，适用于长度不固定的字段
  - 用户名、邮箱、URL 等
  - 需要指定最大长度
  
- **CHAR**：定长字符串，适用于长度固定的字段
  - 验证码、状态码、国家代码等
  - 性能略优于 VARCHAR

#### 常用字段长度建议
| 字段类型 | 推荐长度 | 说明 | 示例 |
|---------|---------|------|------|
| 用户名/账号 | VARCHAR(50) | 足够容纳大多数用户名 | `username VARCHAR(50)` |
| 邮箱 | VARCHAR(100) | RFC 5321 标准最大 254，100 够用 | `email VARCHAR(100)` |
| 密码哈希 | VARCHAR(255) | BCrypt 哈希固定 60 字符，留余量 | `password_hash VARCHAR(255)` |
| 昵称 | VARCHAR(50) | 一般昵称不会太长 | `nickname VARCHAR(50)` |
| URL | VARCHAR(500) |  accommodating 长 URL | `avatar_url VARCHAR(500)` |
| IP 地址 | VARCHAR(45) | IPv6 最长 45 字符 | `ip_address VARCHAR(45)` |
| User Agent | VARCHAR(500) | 浏览器 UA 可能很长 | `user_agent VARCHAR(500)` |
| 短文本 | VARCHAR(200) | 简短描述、原因等 | `failure_reason VARCHAR(200)` |
| 中长文本 | VARCHAR(500) | 详细说明 | `request_reason VARCHAR(500)` |

### 2.3 数值类型

#### TINYINT
- **范围**：-128 到 127（有符号），0 到 255（无符号）
- **用途**：状态标记、布尔值
- **示例**：
```sql
status TINYINT DEFAULT 1 COMMENT '状态：0-禁用, 1-正常, 2-待注销'
used TINYINT(1) DEFAULT 0 COMMENT '是否已使用：0-未使用, 1-已使用'
```

#### INT / BIGINT
- **INT**：普通整数，范围约 ±21 亿
- **BIGINT**：大整数，用于 ID、时间戳等
- **示例**：
```sql
user_id BIGINT NOT NULL COMMENT '用户ID'
```

### 2.4 枚举类型 ENUM
- **用途**：字段值固定且数量较少时使用
- **优点**：节省存储空间，提高查询效率
- **缺点**：修改枚举值需要 ALTER TABLE
- **示例**：
```sql
role ENUM('USER', 'VIP', 'ADMIN') DEFAULT 'USER' COMMENT '角色'
type ENUM('REGISTER', 'LOGIN', 'CHANGE_EMAIL') NOT NULL COMMENT '验证码类型'
```

**注意**：
- 枚举值使用大写英文
- 必须提供 DEFAULT 值
- 添加清晰的 COMMENT 说明每个值的含义

### 2.5 时间类型

#### TIMESTAMP vs DATETIME
- **TIMESTAMP**：
  - 范围：1970-01-01 到 2038-01-19
  - 自动时区转换（受数据库时区配置影响）
  - 占用 4 字节
  - **限制**：在某些时区下，默认值 `'1970-01-01 00:00:01'` 可能会因为转换为 UTC 后超出范围而报错。
  
- **DATETIME**：
  - 范围：1000-01-01 到 9999-12-31
  - 不进行时区转换，存储的是绝对时间
  - 占用 8 字节
  - **推荐场景**：需要存储特殊占位符时间（如逻辑删除标记 `1970-01-01`）或业务预约时间。

**本项目规范（符合阿里 NOT NULL 要求）**：
```sql
-- 审计字段：使用 DATETIME 配合 CURRENT_TIMESTAMP
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'

-- 逻辑删除/占位符字段：使用 DATETIME 配合特殊时间戳
deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '注销时间（1970-01-01表示未删除）'
last_login_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '最后登录时间'
```

### 2.6 布尔类型
MySQL 没有原生 BOOLEAN 类型，使用 `TINYINT(1)` 代替：
```sql
is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活：0-否, 1-是'
used TINYINT(1) DEFAULT 0 COMMENT '是否已使用：0-未使用, 1-已使用'
```

---

## 三、约束规范

### 3.1 NOT NULL 约束（强制）
- **原则**：**表中所有字段必须都是 NOT NULL 属性**，业务可以根据需要定义 DEFAULT 值。
- **原因**：
  - 避免 NULL 值带来的查询复杂性（如 `IS NULL` 判断）。
  - 提高索引效率，NULL 值在 B-Tree 索引中处理效率较低。
  - 聚合函数（COUNT, SUM）统计更准确。
  - 减少 Java 代码中的空指针异常风险。

```sql
-- ✅ 推荐：字符串用空串，时间用特殊值
nickname VARCHAR(50) NOT NULL DEFAULT '' COMMENT '昵称'
deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '注销时间'

-- ❌ 禁止：使用 NULL
nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称'
```

### 3.2 UNIQUE 约束
- **用途**：确保字段值唯一
- **适用场景**：用户名、邮箱、账号等
- **示例**：
```sql
username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名'
email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱地址'
```

### 3.3 DEFAULT 约束
- **原则**：所有字段都应该有默认值（除非业务不允许）
- **常见默认值**：
  - 字符串：`DEFAULT ''` 或 `DEFAULT NULL`
  - 数值：`DEFAULT 0` 或 `DEFAULT 1`
  - 时间：`DEFAULT CURRENT_TIMESTAMP`
  - 枚举：`DEFAULT '第一个值'`

```sql
role ENUM('USER', 'VIP', 'ADMIN') DEFAULT 'USER' COMMENT '角色'
status TINYINT DEFAULT 1 COMMENT '状态'
```

### 3.4 FOREIGN KEY 约束
- **用途**：维护表之间的引用完整性
- **示例**：
```sql
FOREIGN KEY (user_id) REFERENCES ior_users(id) ON DELETE CASCADE
```

**级联操作选择**：
- `ON DELETE CASCADE`：删除主表记录时，自动删除关联记录
- `ON DELETE SET NULL`：删除主表记录时，将外键设为 NULL
- `ON DELETE RESTRICT`：禁止删除有关联记录的主表记录

---

## 四、索引规范

### 4.1 必须创建索引的场景
1. **主键**：自动创建
2. **外键**：手动创建索引
3. **频繁查询的字段**：WHERE 条件中的字段
4. **排序字段**：ORDER BY 中的字段
5. **分组字段**：GROUP BY 中的字段
6. **唯一约束字段**：自动创建唯一索引

### 4.2 索引设计原则
- **联合索引**：遵循最左前缀原则
- **选择性高的字段优先**：如 email 比 status 更适合做索引
- **避免过度索引**：每个索引都会降低写入性能
- **覆盖索引**：尽量让索引包含查询所需的所有字段

### 4.3 示例
```sql
-- 单列索引
INDEX idx_email (email)

-- 联合索引（注意字段顺序）
INDEX idx_email_type (email, type)

-- 常用查询优化
INDEX idx_user_id_created_at (user_id, created_at)
```

---

## 五、注释规范

### 5.1 表注释
```sql
CREATE TABLE ior_users (
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';
```

### 5.2 字段注释
- **格式**：`COMMENT '字段说明（可选：取值范围/枚举值说明）'`
- **要求**：每个字段都必须有注释
- **示例**：
```sql
role ENUM('USER', 'VIP', 'ADMIN') DEFAULT 'USER' COMMENT '角色：USER普通用户, VIP会员, ADMIN管理员'
status TINYINT DEFAULT 1 COMMENT '状态：0-禁用, 1-正常, 2-待注销（冷静期）'
```

---

## 六、字符集和引擎规范

### 6.1 字符集
- **推荐**：`utf8mb4`
- **排序规则**：`utf8mb4_unicode_ci`（不区分大小写）
- **原因**：
  - 支持完整的 Unicode 字符（包括 emoji）
  - 兼容性好

```sql
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

### 6.2 存储引擎
- **推荐**：`InnoDB`
- **原因**：
  - 支持事务
  - 支持外键
  - 行级锁，并发性能好
  - MySQL 5.5+ 默认引擎

---

## 七、逻辑删除规范

### 7.1 软删除字段
```sql
deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '注销时间（逻辑删除标记）'
```

### 7.2 使用规则
- **未删除**：`deleted_at = '1970-01-01 00:00:01'`
- **已删除**：`deleted_at > '1970-01-01 00:00:01'`
- **查询时过滤**：
```sql
SELECT * FROM ior_users WHERE deleted_at = '1970-01-01 00:00:01';
```

### 7.3 优势
- 数据可恢复
- 保留历史记录
- 避免外键约束问题

---

## 八、常见字段模板

### 8.1 标准审计字段（每张表都应包含）
```sql
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

### 8.2 逻辑删除字段（需要软删除的表）
```sql
deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '删除时间'
-- 查询时使用：WHERE deleted_at = '1970-01-01 00:00:01'
```

### 8.3 状态字段
```sql
status TINYINT DEFAULT 1 COMMENT '状态：0-禁用, 1-正常, 2-其他'
```

### 8.4 外键字段
```sql
user_id BIGINT NOT NULL COMMENT '用户ID'
INDEX idx_user_id (user_id)
```

---

## 九、性能优化建议

### 9.1 避免的设计
- ❌ 使用 `SELECT *`，应该明确指定字段
- ❌ 在字段上使用函数导致索引失效
- ❌ 过度使用 TEXT/BLOB 类型
- ❌ 单个表字段过多（建议不超过 50 个）

### 9.2 推荐的做法
- ✅ 合理使用索引
- ✅ 分页查询使用 LIMIT
- ✅ 大数据量表考虑分区
- ✅ 定期分析和优化表
- ✅ 监控慢查询日志

---

## 十、示例：完整建表语句

```sql
CREATE TABLE ior_example (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    status TINYINT DEFAULT 1 COMMENT '状态：0-草稿, 1-发布, 2-下架',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    published_at TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
    deleted_at TIMESTAMP NULL DEFAULT NULL COMMENT '删除时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES ior_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='示例表';
```

---

## 附录：常用数据类型速查表

| 数据类型 | 用途 | 示例 |
|---------|------|------|
| BIGINT | 主键 ID、外键 | `id BIGINT PRIMARY KEY` |
| VARCHAR(n) | 变长字符串 | `username VARCHAR(50)` |
| CHAR(n) | 定长字符串 | `code CHAR(6)` |
| TINYINT | 状态、布尔值 | `status TINYINT DEFAULT 1` |
| INT | 普通整数 | `view_count INT DEFAULT 0` |
| TEXT | 长文本 | `content TEXT` |
| DATETIME | 时间日期（推荐） | `created_at DATETIME` |
| ENUM | 枚举值 | `role ENUM('USER', 'ADMIN')` |
| DECIMAL(m,n) | 精确小数（金额） | `price DECIMAL(10,2)` |

---

**文档版本**：v1.0  
**最后更新**：2026-05-18  
**维护者**：IOR 开发团队
