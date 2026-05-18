-- ============================================
-- 用户模块数据库表设计
-- ============================================

-- 1. 用户主表
CREATE TABLE ior_users (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                           account VARCHAR(50) NOT NULL UNIQUE COMMENT '用户账号（唯一，不可修改）',
                           username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（唯一，不可修改）',
                           email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱地址',
                           password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值（BCrypt加密）',
                           nickname VARCHAR(50) NOT NULL DEFAULT '' COMMENT '昵称（可修改）',
                           avatar_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL（存储于R2/S3）',
                           role ENUM('USER', 'VIP', 'ADMIN') DEFAULT 'USER' COMMENT '角色：USER普通用户, VIP会员, ADMIN管理员',
                           status TINYINT DEFAULT 1 COMMENT '状态：0-禁用, 1-正常, 2-待注销（冷静期）',
                           last_login_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '最后登录时间',
                           last_login_ip VARCHAR(45) NOT NULL DEFAULT '' COMMENT '最后登录IP地址',
                           deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '注销时间（逻辑删除标记，1970-01-01表示未删除）',
                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           INDEX idx_email (email),
                           INDEX idx_account (account),
                           INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 2. 验证码表
CREATE TABLE ior_verification_codes (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
                                        email VARCHAR(100) NOT NULL COMMENT '接收验证码的邮箱',
                                        code VARCHAR(10) NOT NULL COMMENT '验证码（6位数字）',
                                        type ENUM('REGISTER', 'LOGIN', 'CHANGE_EMAIL', 'RESET_PASSWORD') NOT NULL COMMENT '验证码类型：REGISTER注册, LOGIN登录, CHANGE_EMAIL修改邮箱, RESET_PASSWORD找回密码',
                                        used TINYINT(1) DEFAULT 0 COMMENT '是否已使用：0-未使用, 1-已使用',
                                        expires_at DATETIME NOT NULL COMMENT '过期时间',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        INDEX idx_email_type (email, type),
                                        INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码记录表';

-- 3. 用户注销申请表
CREATE TABLE ior_user_deletion_requests (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
                                            user_id BIGINT NOT NULL COMMENT '用户ID',
                                            request_reason VARCHAR(500) NOT NULL DEFAULT '' COMMENT '注销原因（可选）',
                                            status ENUM('PENDING', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING' COMMENT '状态：PENDING待处理, CANCELLED已取消, COMPLETED已完成',
                                            requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
                                            cancelled_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '取消时间',
                                            completed_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '完成时间（7天后）',
                                            FOREIGN KEY (user_id) REFERENCES ior_users(id) ON DELETE CASCADE,
                                            INDEX idx_user_id (user_id),
                                            INDEX idx_status (status),
                                            INDEX idx_completed_at (completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户注销申请表';

-- 4. 用户登录日志表
CREATE TABLE ior_user_login_logs (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
                                     user_id BIGINT NOT NULL COMMENT '用户ID',
                                     login_type ENUM('PASSWORD', 'EMAIL_CODE') NOT NULL COMMENT '登录方式：PASSWORD密码登录, EMAIL_CODE邮箱验证码登录',
                                     ip_address VARCHAR(45) NOT NULL COMMENT '登录IP地址',
                                     user_agent VARCHAR(500) NOT NULL DEFAULT '' COMMENT '用户代理（浏览器信息）',
                                     device_info VARCHAR(200) NOT NULL DEFAULT '' COMMENT '设备信息',
                                     location VARCHAR(100) NOT NULL DEFAULT '' COMMENT '登录地点（可选）',
                                     status TINYINT DEFAULT 1 COMMENT '登录状态：0-失败, 1-成功',
                                     failure_reason VARCHAR(200) NOT NULL DEFAULT '' COMMENT '失败原因（登录失败时记录）',
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
                                     INDEX idx_user_id (user_id),
                                     INDEX idx_created_at (created_at),
                                     INDEX idx_ip_address (ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';