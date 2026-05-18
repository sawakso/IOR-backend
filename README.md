# IOR
> 流媒体社交平台项目后端 (开发中)

## 🛠️ 技术栈
*   **核心框架**: Spring Boot 3.5.14, Java 17
*   **数据库**: MySQL 8.0+, MyBatis-Plus 3.5.9
*   **缓存**: Redis (Lettuce), 策略模式支持降级至 MySQL
*   **安全认证**: Spring Security, JWT (jjwt)
*   **接口文档**: Knife4j (OpenAPI 3)
*   **工具库**: Hutool, Lombok, AWS S3 SDK (R2 兼容)
*   **消息队列**: Kafka / RabbitMQ (待集成)
*   **日志监控**: ELK (待集成)

## 📂 项目结构
```
IOR/
├── docs/                      # 项目文档
│   ├── requirements.md        # 需求规格说明书
│   └── database_standards.md  # 数据库设计与字段规范
├── sql/                       # 数据库脚本
│   └── 1_table_example.sql    # 用户模块表结构（含注销申请表、日志表等）
├── devlog/                    # 开发日志
│   └── 2026.5.18.md           # 今日开发进度记录
├── src/main/java/com/ior/
│   ├── config/                # 配置类
│   │   ├── SecurityConfig.java      # Spring Security 与 JWT 过滤器配置
│   │   └── MybatisPlusConfig.java   # MP 分页与自动填充配置
│   ├── controller/            # 控制器层
│   │   └── IorUsersController.java  # 用户注册、登录、资料修改接口
│   ├── domain/                # 领域模型
│   │   ├── dto/               # 数据传输对象 (RegisterRequest, LoginRequest...)
│   │   ├── entity/            # 数据库实体 (IorUsers, IorVerificationCode...)
│   │   └── vo/                # 视图对象 (Result 统一返回结果)
│   ├── filter/                # 过滤器
│   │   └── JwtAuthenticationFilter.java # JWT 身份校验过滤器
│   ├── mapper/                # MyBatis-Plus Mapper 接口
│   ├── service/               # 业务逻辑层
│   │   ├── impl/              # 服务实现类
│   │   └── MailService.java   # 邮件发送服务
│   ├── strategy/              # 策略模式实现
│   │   ├── VerificationCodeStrategy.java # 验证码存储策略接口
│   │   ├── UserDeletionStrategy.java     # 用户注销处理策略接口
│   │   └── impl/              # 具体策略实现 (Redis/DB, 普通用户/VIP注销)
│   ├── task/                  # 定时任务
│   │   └── UserDeletionTask.java        # 7天冷静期自动注销任务
│   ├── utils/                 # 工具类
│   │   ├── JwtUtil.java       # JWT 生成与解析
│   │   ├── JwtHelper.java     # 便捷获取当前用户信息
│   │   └── RedisConstants.java # Redis Key 常量定义
│   └── IorApplication.java    # 启动类
└── src/main/resources/
    ├── templates/mail/        # 邮件 HTML 模板 (注册、登录、改密等)
    └── application.yaml       # 核心配置文件
```

## 🚀 核心功能
1.  **用户认证**: 支持账号/邮箱/用户名多标识符登录，集成 BCrypt 密码加密。
2.  **验证码系统**: 基于策略模式实现 Redis/MySQL 自动切换，支持注册、登录等多种场景。
3.  **安全管理**: 邮箱修改需双重验证，账户注销设有 7 天冷静期并可自动撤销。
4.  **自动化运维**: 每日凌晨自动扫描并执行过期账户的逻辑删除。