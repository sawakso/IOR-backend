-- ============================================
-- 生命流模块 - 完整表结构（含 media_urls）
-- 执行前建议先删除旧表
-- ============================================

-- 删除旧表（按依赖顺序）
DROP TABLE IF EXISTS ior_post_categories;
DROP TABLE IF EXISTS ior_post_media;
DROP TABLE IF EXISTS ior_post_versions;
DROP TABLE IF EXISTS ior_posts;

-- 1. 帖子主表（包含 media_urls 字段）
CREATE TABLE ior_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父帖子ID（0表示根帖子）',
    
    -- 文本内容
    title VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题',
    content TEXT COMMENT '正文内容',
    media_urls TEXT COMMENT '媒体文件URL数组（JSON格式）',
    
    -- 元数据
    visibility ENUM('PUBLIC', 'PRIVATE', 'DRAFT') DEFAULT 'DRAFT' COMMENT '可见性：PUBLIC-公开, PRIVATE-私密, DRAFT-草稿',
    status TINYINT DEFAULT 1 COMMENT '状态：0-删除, 1-正常',
    tags VARCHAR(500) DEFAULT '' COMMENT '标签（逗号分隔）',
    category_id BIGINT DEFAULT 0 COMMENT '分类ID',
    
    -- 版本管理
    version_count INT DEFAULT 1 COMMENT '总版本数量',
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

-- 2. 帖子版本表（存文本快照）
CREATE TABLE ior_post_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    post_id BIGINT NOT NULL COMMENT '关联的帖子ID',
    version_number INT NOT NULL COMMENT '版本号（从1开始递增）',
    
    -- 文本快照
    title VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题快照',
    content TEXT COMMENT '内容快照',
    media_urls TEXT COMMENT '媒体URL快照（JSON格式）',
    
    change_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '修改说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
    created_by BIGINT NOT NULL COMMENT '修改者ID',
    
    INDEX idx_post_id (post_id),
    INDEX idx_post_version (post_id, version_number),
    UNIQUE KEY uk_post_version (post_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子版本历史表';

-- 3. 媒体文件表（独立存储，可选使用）
CREATE TABLE ior_post_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '媒体ID',
    post_id BIGINT NOT NULL COMMENT '关联的帖子ID',
    version_id BIGINT NOT NULL COMMENT '关联的版本ID',
    
    -- 文件信息
    file_url VARCHAR(500) NOT NULL COMMENT '文件URL（R2存储地址）',
    file_type ENUM('IMAGE', 'VIDEO') NOT NULL COMMENT '文件类型',
    file_format VARCHAR(10) NOT NULL COMMENT '文件格式（jpg, png, mp4等）',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    
    -- 图片特有字段
    width INT DEFAULT 0 COMMENT '图片宽度（像素）',
    height INT DEFAULT 0 COMMENT '图片高度（像素）',
    thumbnail_url VARCHAR(500) DEFAULT '' COMMENT '缩略图URL',
    
    -- 视频特有字段
    duration INT DEFAULT 0 COMMENT '视频时长（秒）',
    
    -- 排序和描述
    sort_order INT DEFAULT 0 COMMENT '排序权重（越小越靠前）',
    description VARCHAR(200) DEFAULT '' COMMENT '文件描述',
    
    -- 状态
    status TINYINT DEFAULT 1 COMMENT '状态：0-删除, 1-正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_post_id (post_id),
    INDEX idx_version_id (version_id),
    INDEX idx_file_type (file_type),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子媒体文件表';

-- 4. 帖子分类表
CREATE TABLE ior_post_categories (
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
INSERT INTO ior_post_categories (name, description, parent_id, sort_order, is_active) VALUES
('日常', '日常生活记录', 0, 1, 1),
('工作', '工作内容相关', 0, 2, 1),
('学习', '学习笔记和心得', 0, 3, 1),
('旅行', '旅行见闻和照片', 0, 4, 1),
('思考', '思考和感悟', 0, 5, 1),
('创作', '原创作品展示', 0, 6, 1);

-- 验证表是否创建成功
SELECT 'Tables created successfully!' AS result;
SHOW TABLES LIKE 'ior_post%';
