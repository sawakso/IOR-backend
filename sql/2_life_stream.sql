-- ============================================
-- 生命流模块数据库表结构
-- 创建时间: 2026-05-19
-- ============================================

-- 1. 帖子主表
CREATE TABLE IF NOT EXISTS ior_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父帖子ID（0表示根帖子）',
    
    -- 当前版本的内容（冗余字段，方便快速查询）
    title VARCHAR(200) NOT NULL DEFAULT '' COMMENT '当前标题',
    content TEXT COMMENT '当前内容',
    media_urls JSON COMMENT '当前媒体文件URL数组',
    
    visibility ENUM('PUBLIC', 'PRIVATE') DEFAULT 'PUBLIC' COMMENT '可见性',
    status TINYINT DEFAULT 1 COMMENT '状态：0-删除, 1-正常',
    tags VARCHAR(500) DEFAULT '' COMMENT '标签（逗号分隔）',
    category_id BIGINT DEFAULT 0 COMMENT '分类ID',
    
    -- 版本管理字段
    version_count INT DEFAULT 1 COMMENT '总版本数量',
    current_version_id BIGINT COMMENT '当前版本的ID（关联版本表）',
    
    deleted_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:01' COMMENT '删除时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_visibility (visibility),
    INDEX idx_category_id (category_id),
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子主表';

-- 2. 帖子版本表（核心表 - 存储所有历史版本）
CREATE TABLE IF NOT EXISTS ior_post_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    post_id BIGINT NOT NULL COMMENT '关联的帖子ID',
    version_number INT NOT NULL COMMENT '版本号（从1开始递增）',
    
    -- 该版本的完整内容快照
    title VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题快照',
    content TEXT COMMENT '内容快照',
    media_urls JSON COMMENT '媒体文件URL快照',
    
    change_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '修改说明（用户填写）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
    created_by BIGINT NOT NULL COMMENT '修改者ID',
    
    INDEX idx_post_id (post_id),
    INDEX idx_post_version (post_id, version_number),
    UNIQUE KEY uk_post_version (post_id, version_number) -- 防止版本号重复
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子版本历史表';

-- 3. 帖子分类表
CREATE TABLE IF NOT EXISTS ior_post_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) NOT NULL DEFAULT '' COMMENT '分类描述',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID（0表示一级分类）',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用, 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子分类表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认分类
INSERT INTO ior_post_categories (name, description, parent_id, sort_order) VALUES
('日常', '日常生活记录', 0, 1),
('工作', '工作内容相关', 0, 2),
('学习', '学习笔记和心得', 0, 3),
('旅行', '旅行见闻和照片', 0, 4),
('思考', '思考和感悟', 0, 5),
('创作', '原创作品展示', 0, 6);
