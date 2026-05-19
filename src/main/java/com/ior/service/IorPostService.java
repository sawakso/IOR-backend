package com.ior.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ior.domain.entity.IorPost;
import com.ior.domain.vo.Result;

public interface IorPostService extends IService<IorPost> {

    /**
     * 创建帖子
     */
    Result createPost(Long userId, String title, String content, String mediaUrls, 
                     String visibility, String tags, Long categoryId);

    /**
     * 修改帖子（自动生成新版本）
     */
    Result updatePost(Long postId, Long userId, String title, String content, 
                     String mediaUrls, String changeSummary);

    /**
     * 获取帖子详情（当前版本）
     */
    Result getPostDetail(Long postId, Long currentUserId);

    /**
     * 获取版本列表
     */
    Result getVersionList(Long postId, Long currentUserId);

    /**
     * 获取版本详情
     */
    Result getVersionDetail(Long versionId, Long currentUserId);

    /**
     * 恢复到指定版本
     */
    Result restoreVersion(Long postId, Long versionId, Long userId);

    /**
     * 删除帖子（软删除）
     */
    Result deletePost(Long postId, Long userId);
}
